package net.neoforged.gradle.common.extensions.obfuscation;

import com.google.common.collect.Maps;
import net.minecraftforge.gdi.ConfigurableDSLElement;
import net.neoforged.gradle.common.tasks.ArtifactFromOutput;
import net.neoforged.gradle.common.tasks.ObfuscatedDependencyMarker;
import net.neoforged.gradle.common.util.TaskDependencyUtils;
import net.neoforged.gradle.common.util.exceptions.MultipleDefinitionsFoundException;
import net.neoforged.gradle.dsl.common.extensions.dependency.replacement.DependencyReplacement;
import net.neoforged.gradle.dsl.common.util.DistributionType;
import net.neoforged.gradle.dsl.common.util.GameArtifact;
import net.neoforged.gradle.dsl.common.util.NamingConstants;
import net.neoforged.gradle.dsl.common.extensions.Mappings;
import net.neoforged.gradle.dsl.common.extensions.MinecraftArtifactCache;
import net.neoforged.gradle.dsl.common.extensions.obfuscation.Obfuscation;
import net.neoforged.gradle.dsl.common.extensions.obfuscation.ObfuscationTarget;
import net.neoforged.gradle.dsl.common.runtime.definition.Definition;
import net.neoforged.gradle.dsl.common.runtime.extensions.CommonRuntimes;
import net.neoforged.gradle.dsl.common.runtime.naming.TaskBuildingContext;
import net.neoforged.gradle.dsl.common.runtime.tasks.Runtime;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import net.neoforged.gradle.dsl.common.util.CommonRuntimeUtils;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.NamedDomainObjectContainer;
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
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class ObfuscationExtension implements ConfigurableDSLElement<Obfuscation>, Obfuscation {

    private static final Logger LOGGER = LoggerFactory.getLogger(ObfuscationExtension.class);

    private final Project project;
    private final Set<String> jarTasksWithObfuscation = new HashSet<>();
    private final NamedDomainObjectContainer<ObfuscationTarget> manualObfuscationTargets;

    @SuppressWarnings("unchecked")
    @Inject
    public ObfuscationExtension(Project project) {
        this.project = project;

        this.manualObfuscationTargets = project.getObjects().domainObjectContainer(
                ObfuscationTarget.class,
                name -> project.getObjects().newInstance(
                        ObfuscationTargetImpl.class,
                        getProject(),
                        name
                )
        );

        getCreateAutomatically().convention(project.provider(() -> (CommonRuntimes<?,?,?>) project.getExtensions().getByType(CommonRuntimes.class)).flatMap(CommonRuntimes::getRuntimes).map(runtimes -> !runtimes.isEmpty()));

        final DependencyReplacement dependencyReplacementsExtension = project.getExtensions().getByType(DependencyReplacement.class);
        dependencyReplacementsExtension.afterDefinitionBake(evaluatedProject -> {
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
    public NamedDomainObjectContainer<ObfuscationTarget> getTargets() {
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

        if (runtimeDefinition == null)
            throw new IllegalArgumentException("No minecraft runtime was configured in a configuration that is consumed by: " + jarTask.getName() + ". Disable automatic configuration, and manually configure your obfuscation targets!");

        if (minecraftVersionString != null) {
            configuredMappingVersionData.put(NamingConstants.Version.MINECRAFT_VERSION, minecraftVersionString);
        }

        final TaskProvider<? extends WithOutput> devArtifactProvider = project.getTasks().register("provideDevelop" + StringUtils.capitalize(jarTask.getName()), ArtifactFromOutput.class, task -> {
            task.dependsOn(jarTask);

            task.getInput().set(jarTask.flatMap(Jar::getArchiveFile));
            task.getOutput().set(project.getLayout().getBuildDirectory().dir("libs").flatMap(directory -> directory.file(jarTask.flatMap(jar -> jar.getArchiveFileName().map(fileName -> fileName.substring(0, fileName.length() - 4) + "-dev.jar")))));
        });

        final Set<TaskProvider<? extends Runtime>> additionalRuntimeTasks = new HashSet<>();

        Map<GameArtifact, TaskProvider<? extends WithOutput>> gameArtifactTasks;
        if (runtimeDefinition != null) {
            gameArtifactTasks = runtimeDefinition.getGameArtifactProvidingTasks();
        } else {
            gameArtifactTasks = artifactCache.cacheGameVersionTasks(
                    getProject(),
                    Objects.requireNonNull(minecraftVersionString), Objects.requireNonNull(distributionType == null ? null : distributionType.getOrNull())
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
                runtimeDefinition,
                runtimeDefinition.getListLibrariesTaskProvider()
        );

        final TaskProvider<? extends Runtime> obfuscator = mappingsExtension.getChannel().get().getUnapplyCompiledMappingsTaskBuilder().get().build(context);
        obfuscator.configure(task -> task.dependsOn(devArtifactProvider));
        runtimeDefinition.configureAssociatedTask(obfuscator);

        final TaskProvider<? extends WithOutput> markerGenerator = project.getTasks().register(CommonRuntimeUtils.buildTaskName(jarTask, "markObfuscated"), ObfuscatedDependencyMarker.class, task -> {
            task.dependsOn(obfuscator);

            task.getObfuscatedJar().set(obfuscator.flatMap(WithOutput::getOutput));
            task.getOutputFileName().set(obfuscator.flatMap(WithOutput::getOutputFileName));
            task.getOutput().set(project.getLayout().getBuildDirectory().file(obfuscator.flatMap(WithOutput::getOutputFileName).map(fileName -> "libs/" + fileName)));
        });

        obfuscator.configure(task -> task.finalizedBy(markerGenerator));
        jarTask.configure(task -> task.finalizedBy(obfuscator));

        final Definition<?> finalRuntimeDefinition = runtimeDefinition;
        additionalRuntimeTasks.forEach(finalRuntimeDefinition::configureAssociatedTask);
    }
}
