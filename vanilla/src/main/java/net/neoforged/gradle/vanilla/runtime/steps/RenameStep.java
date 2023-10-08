package net.neoforged.gradle.vanilla.runtime.steps;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import net.neoforged.gradle.common.runtime.naming.tasks.ApplyOfficialMappingsToCompiledJar;
import net.neoforged.gradle.common.util.MappingUtils;
import net.neoforged.gradle.common.util.TaskDependencyUtils;
import net.neoforged.gradle.common.util.exceptions.MultipleDefinitionsFoundException;
import net.neoforged.gradle.dsl.common.util.CacheableMinecraftVersion;
import net.neoforged.gradle.dsl.common.util.GameArtifact;
import net.neoforged.gradle.util.RenameConstants;
import net.neoforged.gradle.dsl.common.util.NamingConstants;
import net.neoforged.gradle.dsl.common.extensions.Mappings;
import net.neoforged.gradle.dsl.common.runtime.naming.TaskBuildingContext;
import net.neoforged.gradle.dsl.common.runtime.tasks.Runtime;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import net.neoforged.gradle.dsl.common.util.CommonRuntimeUtils;
import net.neoforged.gradle.vanilla.runtime.VanillaRuntimeDefinition;
import org.gradle.api.file.RegularFile;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class RenameStep implements IStep {

    @Override
    public TaskProvider<? extends Runtime> buildTask(VanillaRuntimeDefinition definition, TaskProvider<? extends WithOutput> inputProvidingTask, @NotNull File minecraftCache, @NotNull File workingDirectory, @NotNull Map<String, TaskProvider<? extends WithOutput>> pipelineTasks, @NotNull Map<GameArtifact, TaskProvider<? extends WithOutput>> gameArtifactTasks, @NotNull Consumer<TaskProvider<? extends Runtime>> additionalTaskConfigurator) {
        final Mappings mappingsExtension = definition.getSpecification().getProject().getExtensions().getByType(Mappings.class);
        final Map<String, String> mappingVersionData = Maps.newHashMap();
        mappingVersionData.put(NamingConstants.Version.VERSION, definition.getSpecification().getMinecraftVersion());
        mappingVersionData.put(NamingConstants.Version.MINECRAFT_VERSION, definition.getSpecification().getMinecraftVersion());
        mappingVersionData.putAll(mappingsExtension.getVersion().get());

        final TaskProvider<? extends WithOutput> artifact = inputProvidingTask;

        final Set<TaskProvider<? extends Runtime>> additionalTasks = Sets.newHashSet();
        final TaskBuildingContext context = new TaskBuildingContext(
                definition.getSpecification().getProject(), "mapGame", taskName -> CommonRuntimeUtils.buildTaskName(definition.getSpecification(), taskName), artifact, definition.getGameArtifactProvidingTasks(), mappingVersionData, additionalTasks, definition
        );

        final TaskProvider<? extends Runtime> namingTask = buildApplyCompiledMappingsTask(context);
        additionalTasks.forEach(additionalTaskConfigurator);

        namingTask.configure(
                task -> {
                    task.getArguments().putAll(CommonRuntimeUtils.buildArguments(definition, RenameConstants.DEFAULT_RENAME_VALUES, pipelineTasks, task, Optional.of(artifact)));
                    task.getOutput().set(task.getOutputDirectory().file("output.jar"));
                }
        );

        return namingTask;
    }
    
    private @NotNull TaskProvider<? extends Runtime> buildApplyCompiledMappingsTask(@NotNull final TaskBuildingContext context) {
        final String ApplyTaskName = CommonRuntimeUtils.buildTaskName(context.getInputTask(), "deobfuscate");
        
        if (!context.getRuntimeDefinition().isPresent())
            throw new IllegalArgumentException("Cannot apply compiled mappings without a runtime definition");
        
        final TaskProvider<? extends WithOutput> librariesTask = context.getLibrariesTask();
        
        if (librariesTask == null) {
            throw new IllegalArgumentException("Cannot apply compiled mappings without a libraries task");
        }
        
        final TaskProvider<ApplyOfficialMappingsToCompiledJar> applyTask = context.getProject().getTasks().register(ApplyTaskName, ApplyOfficialMappingsToCompiledJar.class, task -> {
            task.setGroup("mappings/official");
            task.setDescription("Unapplies the Official mappings and re-obfuscates a compiled jar");
            
            task.getMinecraftVersion()
                    .set(context.getMappingVersion().flatMap(versionData -> {
                        if (versionData.containsKey(NamingConstants.Version.VERSION) || versionData.containsKey(NamingConstants.Version.MINECRAFT_VERSION)) {
                            return context.getProject().provider(() -> CacheableMinecraftVersion.from(MappingUtils.getVersionOrMinecraftVersion(versionData), context.getProject()));
                        } else {
                            return context.getInputTask().map(t -> {
                                try {
                                    return CacheableMinecraftVersion.from(MappingUtils.getVersionOrMinecraftVersion(TaskDependencyUtils.extractRuntimeDefinition(context.getProject(), t).getMappingVersionData()), context.getProject());
                                } catch (MultipleDefinitionsFoundException e) {
                                    throw new RuntimeException("Could not determine the runtime definition to use. Multiple definitions were found: " + e.getDefinitions().stream().map(r1 -> r1.getSpecification().getVersionedName()).collect(Collectors.joining(", ")), e);
                                }
                            });
                        }
                    }));

            task.getInput().set(context.getInputTask().flatMap(WithOutput::getOutput));
            task.getOutput().set(context.getProject().getLayout().getBuildDirectory().dir("obfuscation/" + context.getInputTask().getName()).flatMap(directory -> directory.file(context.getInputTask().flatMap(WithOutput::getOutputFileName).orElse(context.getInputTask().flatMap(WithOutput::getOutput).map(RegularFile::getAsFile).map(File::getName)))));
            task.getLibraries().set(librariesTask.flatMap(WithOutput::getOutput));
            task.getMappings().set(context.getGameArtifactTask(GameArtifact.CLIENT_MAPPINGS).flatMap(WithOutput::getOutput));
            
            task.dependsOn(context.getInputTask());
            task.dependsOn(librariesTask);
        });
        
        context.getInputTask().configure(task -> task.finalizedBy(applyTask));
        
        return applyTask;
    }

    @Override
    public String getName() {
        return "rename";
    }
}
