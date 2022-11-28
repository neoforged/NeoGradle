package net.minecraftforge.gradle.vanilla.runtime.steps;

import net.minecraftforge.gradle.common.extensions.MappingsExtension;
import net.minecraftforge.gradle.common.extensions.MinecraftExtension;
import net.minecraftforge.gradle.common.runtime.naming.NamingChannelProvider;
import net.minecraftforge.gradle.common.runtime.naming.ApplyMappingsTaskBuildingContext;
import net.minecraftforge.gradle.common.runtime.tasks.IRuntimeTask;
import net.minecraftforge.gradle.common.util.GameArtifact;
import net.minecraftforge.gradle.vanilla.runtime.spec.VanillaRuntimeSpec;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class RenameStep implements IStep {

    @Override
    public TaskProvider<? extends IRuntimeTask> buildTask(VanillaRuntimeSpec spec, TaskProvider<? extends IRuntimeTask> inputProvidingTask, @NotNull File minecraftCache, @NotNull Map<String, TaskProvider<? extends IRuntimeTask>> pipelineTasks, @NotNull Map<GameArtifact, File> gameArtifacts, @NotNull Map<GameArtifact, TaskProvider<? extends IRuntimeTask>> gameArtifactTasks, @NotNull Consumer<TaskProvider<? extends IRuntimeTask>> additionalTaskConfigurator) {
        final MinecraftExtension minecraftExtension = spec.project().getExtensions().getByType(MinecraftExtension.class);
        final MappingsExtension mappingsExtension = minecraftExtension.getMappings();

        final NamingChannelProvider namingChannelProvider = mappingsExtension.getMappingChannel().get();
        final Map<String, String> mappingVersionData = mappingsExtension.getMappingVersion().get();

        final ApplyMappingsTaskBuildingContext context = new ApplyMappingsTaskBuildingContext(
                spec, minecraftCache, pipelineTasks, namingChannelProvider, mappingVersionData, inputProvidingTask, gameArtifacts, gameArtifactTasks, Optional.empty()
        );

        final TaskProvider<? extends IRuntimeTask> namingTask = namingChannelProvider.getApplySourceMappingsTaskBuilder().get().build(context);
        context.additionalRuntimeTasks().forEach(additionalTaskConfigurator);

        return namingTask;
    }

    @Override
    public String getName() {
        return "rename";
    }
}
