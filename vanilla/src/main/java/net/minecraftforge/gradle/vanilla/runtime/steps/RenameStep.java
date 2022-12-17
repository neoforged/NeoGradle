package net.minecraftforge.gradle.vanilla.runtime.steps;

import net.minecraftforge.gradle.common.extensions.MappingsExtension;
import net.minecraftforge.gradle.common.extensions.MinecraftExtension;
import net.minecraftforge.gradle.common.runtime.naming.NamingChannelProvider;
import net.minecraftforge.gradle.common.runtime.naming.ApplyMappingsTaskBuildingContext;
import net.minecraftforge.gradle.common.runtime.tasks.IRuntimeTask;
import net.minecraftforge.gradle.common.tasks.ITaskWithOutput;
import net.minecraftforge.gradle.common.util.GameArtifact;
import net.minecraftforge.gradle.vanilla.runtime.VanillaRuntimeDefinition;
import net.minecraftforge.gradle.vanilla.runtime.spec.VanillaRuntimeSpec;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class RenameStep implements IStep {

    @Override
    public TaskProvider<? extends IRuntimeTask> buildTask(VanillaRuntimeDefinition definition, TaskProvider<? extends ITaskWithOutput> inputProvidingTask, @NotNull File minecraftCache, @NotNull Map<String, TaskProvider<? extends ITaskWithOutput>> pipelineTasks, @NotNull Map<GameArtifact, File> gameArtifacts, @NotNull Map<GameArtifact, TaskProvider<? extends ITaskWithOutput>> gameArtifactTasks, @NotNull Consumer<TaskProvider<? extends IRuntimeTask>> additionalTaskConfigurator) {
        final MappingsExtension mappingsExtension = definition.spec().project().getExtensions().getByType(MappingsExtension.class);
        final Map<String, String> mappingVersionData = mappingsExtension.getMappingVersion().get();

        final ApplyMappingsTaskBuildingContext context = new ApplyMappingsTaskBuildingContext(
                definition.spec().project(), "mapGame", inputProvidingTask, mappingVersionData, definition.gameArtifactProvidingTasks()
        );

        final TaskProvider<? extends IRuntimeTask> namingTask = context.namingChannelProvider().getApplySourceMappingsTaskBuilder().get().build(context);
        context.additionalRuntimeTasks().forEach(additionalTaskConfigurator);

        return namingTask;
    }

    @Override
    public String getName() {
        return "rename";
    }
}
