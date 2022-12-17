package net.minecraftforge.gradle.vanilla.runtime.steps;

import net.minecraftforge.gradle.common.runtime.tasks.IRuntimeTask;
import net.minecraftforge.gradle.common.runtime.tasks.ListLibraries;
import net.minecraftforge.gradle.common.tasks.ITaskWithOutput;
import net.minecraftforge.gradle.common.util.CommonRuntimeUtils;
import net.minecraftforge.gradle.common.util.GameArtifact;
import net.minecraftforge.gradle.vanilla.runtime.VanillaRuntimeDefinition;
import net.minecraftforge.gradle.vanilla.runtime.spec.VanillaRuntimeSpec;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class CollectLibraryInformationStep implements IStep {
    @Override
    public TaskProvider<? extends IRuntimeTask> buildTask(VanillaRuntimeDefinition definition, TaskProvider<? extends ITaskWithOutput> inputProvidingTask, @NotNull File minecraftCache, @NotNull Map<String, TaskProvider<? extends ITaskWithOutput>> pipelineTasks, @NotNull Map<GameArtifact, File> gameArtifacts, @NotNull Map<GameArtifact, TaskProvider<? extends ITaskWithOutput>> gameArtifactTasks, @NotNull Consumer<TaskProvider<? extends IRuntimeTask>> additionalTaskConfigurator) {
        return definition.spec().project().getTasks().register(CommonRuntimeUtils.buildTaskName(definition, "libraries"), ListLibraries.class, task -> {
            task.getDownloadedVersionJsonFile().set(gameArtifactTasks.get(GameArtifact.VERSION_MANIFEST).flatMap(ITaskWithOutput::getOutput));
        });
    }

    @Override
    public String getName() {
        return "libraries";
    }
}
