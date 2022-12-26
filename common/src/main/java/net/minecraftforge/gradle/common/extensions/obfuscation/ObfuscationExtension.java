package net.minecraftforge.gradle.common.extensions.obfuscation;

import com.google.common.collect.Maps;
import net.minecraftforge.gradle.common.tasks.ArtifactFromOutput;
import net.minecraftforge.gradle.common.tasks.ObfuscatedDependencyMarker;
import net.minecraftforge.gradle.common.util.CommonRuntimeUtils;
import net.minecraftforge.gradle.common.util.ConfigurableObject;
import net.minecraftforge.gradle.common.util.NamingConstants;
import net.minecraftforge.gradle.common.util.TaskDependencyUtils;
import net.minecraftforge.gradle.common.util.exceptions.MultipleDefinitionsFoundException;
import net.minecraftforge.gradle.dsl.common.extensions.Mappings;
import net.minecraftforge.gradle.dsl.common.extensions.obfuscation.Obfuscation;
import net.minecraftforge.gradle.dsl.common.extensions.obfuscation.ObfuscationTarget;
import net.minecraftforge.gradle.dsl.common.extensions.repository.Repository;
import net.minecraftforge.gradle.dsl.common.runtime.naming.TaskBuildingContext;
import net.minecraftforge.gradle.dsl.common.tasks.WithOutput;
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
import java.util.Set;
import java.util.stream.Collectors;

public abstract class ObfuscationExtension extends ConfigurableObject<Obfuscation> implements Obfuscation {

    private static final Logger LOGGER = LoggerFactory.getLogger(ObfuscationExtension.class);

    private final Project project;
    private final Set<String> jarTasksWithObfuscation = new HashSet<>();
    private final NamedDomainObjectContainer<ObfuscationTarget> manualObfuscationTargets;

    @Inject
    public ObfuscationExtension(Project project) {
        this.project = project;

        this.manualObfuscationTargets = project.container(ObfuscationTarget.class, name -> {
            ObfuscationTargetImpl target = project.getObjects().newInstance(ObfuscationTargetImpl.class, project);
            target.getMinecraftVersion().set(name);
            return target;
        });
        getCreateAutomatically().convention(true);

        final Repository<?,?,?,?,?> repository = project.getExtensions().getByType(Repository.class);
        repository.afterEntryRealisation(evaluatedProject -> {
            manualObfuscationTargets.getAsMap().forEach((name, targetConfig) -> {
                try {
                    final TaskProvider<? extends Jar> taskProvider = evaluatedProject.getTasks().named(name, Jar.class);

                    createObfuscateTask(taskProvider, targetConfig.getMinecraftVersion());
                } catch (UnknownTaskException taskException) {
                    throw new RuntimeException("The task '" + name + "' does not exist. Please create it before using it as an obfuscation target.", taskException);
                }
            });

            if (getCreateAutomatically().get()) {
                evaluatedProject.getTasks().withType(Jar.class).all(jarTask -> {
                    if (!jarTasksWithObfuscation.contains(jarTask.getName())) {
                        createObfuscateTask(evaluatedProject.getTasks().named(jarTask.getName(), Jar.class), null);
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

    private void createObfuscateTask(TaskProvider<? extends Jar> jarTask, @Nullable Property<String> minecraftVersion) {
        jarTasksWithObfuscation.add(jarTask.getName());
        final Mappings mappingsExtension = project.getExtensions().getByType(Mappings.class);

        String minecraftVersionString = minecraftVersion == null ? null : minecraftVersion.getOrNull();
        Map<String, String> configuredMappingVersionData;
        try {
            configuredMappingVersionData = TaskDependencyUtils.realiseTaskAndExtractRuntimeDefinition(getProject(), jarTask).configuredMappingVersionData();
        } catch (MultipleDefinitionsFoundException e) {
            if (minecraftVersion == null) {
                throw new RuntimeException("Could not determine the runtime definition to use. Multiple definitions were found: " + e.getDefinitions().stream().map(r1 -> r1.spec().getName()).collect(Collectors.joining(", ")), e);
            }

            LOGGER.warn("Could not determine the runtime definition to use. Multiple definitions were found: " + e.getDefinitions().stream().map(r1 -> r1.spec().getName()).collect(Collectors.joining(", ")), e);
            LOGGER.warn("Using the manually configured version: " + minecraftVersionString);
            configuredMappingVersionData = Maps.newHashMap();
        }

        if (minecraftVersionString != null) {
            configuredMappingVersionData.put(NamingConstants.Version.MINECRAFT_VERSION, minecraftVersionString);
        }

        //TODO: Handle mapping version detection!
        final TaskProvider<? extends WithOutput> devArtifactProvider = project.getTasks().register("provideDevelop" + StringUtils.capitalize(jarTask.getName()), ArtifactFromOutput.class, task -> {
            task.dependsOn(jarTask);

            task.getInput().set(jarTask.flatMap(Jar::getArchiveFile));
            task.getOutput().set(project.getLayout().getBuildDirectory().dir("libs").flatMap(directory -> directory.file(jarTask.flatMap(jar -> jar.getArchiveFileName().map(fileName -> fileName.substring(0, fileName.length() - 4) + "-dev.jar")))));
        });

        final TaskBuildingContext context = new TaskBuildingContext(
                project,
                CommonRuntimeUtils.buildTaskName(jarTask, "obfuscate"),
                task -> String.format("obfuscate%s", StringUtils.capitalize(jarTask.getName())),
                devArtifactProvider,
                Maps.newHashMap(),
                configuredMappingVersionData,
                new HashSet<>()
        );

        final TaskProvider<? extends WithOutput> obfuscator = mappingsExtension.getChannel().get().getUnapplyCompiledMappingsTaskBuilder().get().apply(context);
        obfuscator.configure(task -> task.dependsOn(devArtifactProvider));

        final TaskProvider<? extends WithOutput> markerGenerator = project.getTasks().register(CommonRuntimeUtils.buildTaskName(jarTask, "markObfuscated"), ObfuscatedDependencyMarker.class, task -> {
            task.dependsOn(obfuscator);

            task.getObfuscatedJar().set(obfuscator.flatMap(WithOutput::getOutput));
            task.getOutputFileName().set(obfuscator.flatMap(WithOutput::getOutputFileName));
            task.getOutput().set(project.getLayout().getBuildDirectory().file("libs/" + obfuscator.flatMap(WithOutput::getOutputFileName)));
        });

        obfuscator.configure(task -> task.finalizedBy(markerGenerator));
        jarTask.configure(task -> task.finalizedBy(obfuscator));
    }
}
