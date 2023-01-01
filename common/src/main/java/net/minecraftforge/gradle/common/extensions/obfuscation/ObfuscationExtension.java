package net.minecraftforge.gradle.common.extensions.obfuscation;

import com.google.common.collect.Maps;
import net.minecraftforge.gradle.common.runtime.extensions.CommonRuntimeExtension;
import net.minecraftforge.gradle.common.tasks.ArtifactFromOutput;
import net.minecraftforge.gradle.common.tasks.ObfuscatedDependencyMarker;
import net.minecraftforge.gradle.common.util.ConfigurableNamedDSLObjectContainer;
import net.minecraftforge.gradle.common.util.ConfigurableObject;
import net.minecraftforge.gradle.common.util.TaskDependencyUtils;
import net.minecraftforge.gradle.common.util.exceptions.MultipleDefinitionsFoundException;
import net.minecraftforge.gradle.dsl.base.util.NamedDSLObjectContainer;
import net.minecraftforge.gradle.dsl.common.extensions.Mappings;
import net.minecraftforge.gradle.dsl.common.extensions.MinecraftArtifactCache;
import net.minecraftforge.gradle.dsl.common.extensions.obfuscation.Obfuscation;
import net.minecraftforge.gradle.dsl.common.extensions.obfuscation.ObfuscationTarget;
import net.minecraftforge.gradle.dsl.common.extensions.repository.Repository;
import net.minecraftforge.gradle.dsl.common.runtime.definition.Definition;
import net.minecraftforge.gradle.dsl.common.runtime.extensions.CommonRuntimes;
import net.minecraftforge.gradle.dsl.common.runtime.naming.TaskBuildingContext;
import net.minecraftforge.gradle.dsl.common.runtime.tasks.Runtime;
import net.minecraftforge.gradle.dsl.common.tasks.WithOutput;
import net.minecraftforge.gradle.dsl.common.util.CommonRuntimeUtils;
import net.minecraftforge.gradle.dsl.common.util.DistributionType;
import net.minecraftforge.gradle.dsl.common.util.GameArtifact;
import net.minecraftforge.gradle.dsl.common.util.NamingConstants;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.NamedDomainObjectFactory;
import org.gradle.api.Project;
import org.gradle.api.UnknownTaskException;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.jvm.tasks.Jar;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class ObfuscationExtension extends ConfigurableObject<Obfuscation> implements Obfuscation {

    private static final Logger LOGGER = LoggerFactory.getLogger(ObfuscationExtension.class);

    private final Project project;
    private final Set<String> jarTasksWithObfuscation = new HashSet<>();
    private final ConfigurableNamedDSLObjectContainer.Simple<ObfuscationTarget> manualObfuscationTargets;

    @SuppressWarnings("unchecked")
    @Inject
    public ObfuscationExtension(Project project) {
        this.project = project;

        this.manualObfuscationTargets = project.getObjects().newInstance(
                ConfigurableNamedDSLObjectContainer.Simple.class,
                getProject(),
                ObfuscationTargetImpl.class,
                (NamedDomainObjectFactory<ObfuscationTarget>) name -> project.getObjects().newInstance(
                        ObfuscationTargetImpl.class,
                        getProject(),
                        name
                )
        );

        getCreateAutomatically().convention(project.provider(() -> (CommonRuntimes<?,?,?>) project.getExtensions().getByType(CommonRuntimes.class)).flatMap(CommonRuntimes::getRuntimes).map(runtimes -> !runtimes.isEmpty()));

        final Repository<?, ?, ?, ?, ?> repository = project.getExtensions().getByType(Repository.class);
        repository.afterEntryRealisation(evaluatedProject -> {
            manualObfuscationTargets.getAsMap().forEach((name, targetConfig) -> {
                try {
                    final TaskProvider<? extends Jar> taskProvider = evaluatedProject.getTasks().named(name, Jar.class);

                    createObfuscateTask(taskProvider, targetConfig.getMinecraftVersion(), targetConfig.getDistributionType());
                } catch (UnknownTaskException taskException) {
                    throw new RuntimeException("The task '" + name + "' does not exist. Please create it before using it as an obfuscation target.", taskException);
                }
            });

            if (getCreateAutomatically().get()) {
                evaluatedProject.getTasks().withType(Jar.class).all(jarTask -> {
                    if (!jarTasksWithObfuscation.contains(jarTask.getName())) {
                        createObfuscateTask(evaluatedProject.getTasks().named(jarTask.getName(), Jar.class), null, null);
                    }
                });
            }
        });
    }


    @Override
    public Project getProject() {
        return project;
    }

    @NotNull
    @Override
    public NamedDSLObjectContainer<?, ObfuscationTarget> getTargets() {
        return manualObfuscationTargets;
    }

    private void createObfuscateTask(TaskProvider<? extends Jar> jarTask, @Nullable Property<String> minecraftVersion, @Nullable Property<DistributionType> distributionType) {
        jarTasksWithObfuscation.add(jarTask.getName());
        final Mappings mappingsExtension = project.getExtensions().getByType(Mappings.class);
        final MinecraftArtifactCache artifactCache = project.getExtensions().getByType(MinecraftArtifactCache.class);

        String minecraftVersionString = minecraftVersion == null ? null : minecraftVersion.getOrNull();
        Definition<?> runtimeDefinition = null;
        Map<String, String> configuredMappingVersionData;
        try {
            runtimeDefinition = TaskDependencyUtils.realiseTaskAndExtractRuntimeDefinition(getProject(), jarTask);
            configuredMappingVersionData = runtimeDefinition.getMappingVersionData();
        } catch (MultipleDefinitionsFoundException e) {
            if (minecraftVersion == null) {
                throw new RuntimeException("Could not determine the runtime definition to use. Multiple definitions were found: " + e.getDefinitions().stream().map(r1 -> r1.getSpecification().getName()).collect(Collectors.joining(", ")), e);
            }

            LOGGER.warn("Could not determine the runtime definition to use. Multiple definitions were found: " + e.getDefinitions().stream().map(r1 -> r1.getSpecification().getName()).collect(Collectors.joining(", ")), e);
            LOGGER.warn("Using the manually configured version: " + minecraftVersionString);
            configuredMappingVersionData = Maps.newHashMap();
        }

        if (minecraftVersionString != null) {
            configuredMappingVersionData.put(NamingConstants.Version.MINECRAFT_VERSION, minecraftVersionString);
        }

        final TaskProvider<? extends WithOutput> devArtifactProvider = project.getTasks().register("provideDevelop" + StringUtils.capitalize(jarTask.getName()), ArtifactFromOutput.class, task -> {
            task.dependsOn(jarTask);

            task.getInput().set(jarTask.flatMap(Jar::getArchiveFile));
            task.getOutput().set(project.getLayout().getBuildDirectory().dir("libs").flatMap(directory -> directory.file(jarTask.flatMap(jar -> jar.getArchiveFileName().map(fileName -> fileName.substring(0, fileName.length() - 4) + "-dev.jar")))));
        });

        final Set<TaskProvider<? extends Runtime>> additionalRuntimeTasks = new HashSet<>();

        Map<GameArtifact, TaskProvider<? extends WithOutput>> gameArtifactTasks = Maps.newHashMap();
        if (runtimeDefinition != null) {
            gameArtifactTasks = runtimeDefinition.getGameArtifactProvidingTasks();
        } else {
            gameArtifactTasks = artifactCache.cacheGameVersionTasks(
                    getProject(),
                    new File(getProject().getBuildFile(), "obfuscation-cache"),
                    Objects.requireNonNull(minecraftVersionString),
                    Objects.requireNonNull(distributionType == null ? null : distributionType.getOrNull())
                    );
        }

        final TaskBuildingContext context = new TaskBuildingContext(
                project,
                CommonRuntimeUtils.buildTaskName(jarTask, "obfuscate"),
                task -> String.format("obfuscate%s", StringUtils.capitalize(jarTask.getName())),
                devArtifactProvider,
                gameArtifactTasks,
                configuredMappingVersionData,
                additionalRuntimeTasks,
                runtimeDefinition
        );



        final TaskProvider<? extends WithOutput> obfuscator = mappingsExtension.getChannel().get().getUnapplyCompiledMappingsTaskBuilder().get().build(context);
        obfuscator.configure(task -> task.dependsOn(devArtifactProvider));

        final TaskProvider<? extends WithOutput> markerGenerator = project.getTasks().register(CommonRuntimeUtils.buildTaskName(jarTask, "markObfuscated"), ObfuscatedDependencyMarker.class, task -> {
            task.dependsOn(obfuscator);

            task.getObfuscatedJar().set(obfuscator.flatMap(WithOutput::getOutput));
            task.getOutputFileName().set(obfuscator.flatMap(WithOutput::getOutputFileName));
            task.getOutput().set(project.getLayout().getBuildDirectory().file("libs/" + obfuscator.flatMap(WithOutput::getOutputFileName)));
        });

        obfuscator.configure(task -> task.finalizedBy(markerGenerator));
        jarTask.configure(task -> task.finalizedBy(obfuscator));

        final Definition<?> finalRuntimeDefinition = runtimeDefinition;
        additionalRuntimeTasks.forEach(task -> {
            if (finalRuntimeDefinition != null) {
                finalRuntimeDefinition.configureAssociatedTask(task);
            } else {
                task.configure(t -> CommonRuntimeExtension.configureCommonRuntimeTaskParameters(
                        t,
                        Maps.newHashMap(),
                        t.getName(),
                        distributionType.getOrNull(),
                        minecraftVersionString,
                        getProject(),
                        new File(getProject().getBuildFile(), String.format("obfuscation/%s", t.getName()))
                ));
            }
        });
    }
}
