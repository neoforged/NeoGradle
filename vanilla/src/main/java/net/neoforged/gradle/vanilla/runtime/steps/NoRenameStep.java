package net.neoforged.gradle.vanilla.runtime.steps;

import net.neoforged.gradle.common.runtime.tasks.NoopRuntime;
import net.neoforged.gradle.dsl.common.runtime.tasks.Runtime;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import net.neoforged.gradle.dsl.common.util.CommonRuntimeUtils;
import net.neoforged.gradle.dsl.common.util.GameArtifact;
import net.neoforged.gradle.util.DecompileUtils;
import net.neoforged.gradle.vanilla.runtime.VanillaRuntimeDefinition;
import net.neoforged.gradle.vanilla.runtime.tasks.CleanArtifact;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class NoRenameStep implements IStep
{
    @Override
    public TaskProvider<? extends Runtime> buildTask(
        final VanillaRuntimeDefinition definition,
        final TaskProvider<? extends WithOutput> inputProvidingTask,
        @NotNull final File minecraftCache,
        @NotNull final File workingDirectory,
        @NotNull final Map<String, TaskProvider<? extends WithOutput>> pipelineTasks,
        @NotNull final Map<GameArtifact, TaskProvider<? extends WithOutput>> gameArtifactTasks,
        @NotNull final Consumer<TaskProvider<? extends Runtime>> additionalTaskConfigurator)
    {
        final TaskProvider<? extends WithOutput> artifact = gameArtifactTasks.get(definition.getSpecification().getDistribution().getGameArtifact());

        return definition.getSpecification().getProject().getTasks().register(CommonRuntimeUtils.buildTaskName(definition, "rename"), NoopRuntime.class, task -> {
            task.getInput().set(artifact.flatMap(WithOutput::getOutput));
            CommonRuntimeUtils.buildArguments(task.getArguments(), definition, DecompileUtils.DEFAULT_DECOMPILE_VALUES, pipelineTasks, task, Optional.of(inputProvidingTask));
        });
    }

    @Override
    public String getName()
    {
        return "rename";
    }
}
