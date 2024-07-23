package net.neoforged.gradle.vanilla.runtime.steps;

import net.neoforged.gradle.common.runtime.tasks.ListLibraries;
import net.neoforged.gradle.dsl.common.runtime.tasks.Runtime;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import net.neoforged.gradle.dsl.common.util.CommonRuntimeUtils;
import net.neoforged.gradle.dsl.common.util.GameArtifact;
import net.neoforged.gradle.vanilla.runtime.VanillaRuntimeDefinition;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Map;
import java.util.function.Consumer;

public class CollectLibraryInformationStep implements IStep {
    @Override
    public TaskProvider<? extends Runtime> buildTask(VanillaRuntimeDefinition definition, TaskProvider<? extends WithOutput> inputProvidingTask, @NotNull File minecraftCache, @NotNull File workingDirectory, @NotNull Map<String, TaskProvider<? extends WithOutput>> pipelineTasks, @NotNull Map<GameArtifact, TaskProvider<? extends WithOutput>> gameArtifactTasks, @NotNull Consumer<TaskProvider<? extends Runtime>> additionalTaskConfigurator) {
        return definition.getSpecification().getProject().getTasks().register(CommonRuntimeUtils.buildTaskName(definition, "libraries"), ListLibraries.class, task -> {
            task.getDownloadedVersionJsonFile().set(gameArtifactTasks.get(GameArtifact.VERSION_MANIFEST).flatMap(WithOutput::getOutput));
        });
    }

    @Override
    public String getName() {
        return "libraries";
    }
}
