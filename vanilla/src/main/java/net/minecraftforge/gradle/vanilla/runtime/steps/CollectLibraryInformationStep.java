package net.minecraftforge.gradle.vanilla.runtime.steps;

import net.minecraftforge.gradle.common.runtime.tasks.IRuntimeTask;
import net.minecraftforge.gradle.common.runtime.tasks.ListLibraries;
import net.minecraftforge.gradle.common.tasks.ITaskWithOutput;
import net.minecraftforge.gradle.common.util.CommonRuntimeUtils;
import net.minecraftforge.gradle.common.util.GameArtifact;
import net.minecraftforge.gradle.vanilla.runtime.spec.VanillaRuntimeSpec;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class CollectLibraryInformationStep implements IStep {
    @Override
    public TaskProvider<? extends IRuntimeTask> buildTask(VanillaRuntimeSpec spec, TaskProvider<? extends IRuntimeTask> inputProvidingTask, @NotNull File minecraftCache, @NotNull Map<String, TaskProvider<? extends IRuntimeTask>> pipelineTasks, @NotNull Map<GameArtifact, File> gameArtifacts, @NotNull Map<GameArtifact, TaskProvider<? extends IRuntimeTask>> gameArtifactTasks, @NotNull Consumer<TaskProvider<? extends IRuntimeTask>> additionalTaskConfigurator) {
        return spec.project().getTasks().register(CommonRuntimeUtils.buildTaskName(spec, "libraries"), ListLibraries.class, task -> {
            task.getDownloadedVersionJsonFile().set(gameArtifactTasks.get(GameArtifact.VERSION_MANIFEST).flatMap(ITaskWithOutput::getOutput));
        });
    }

    @Override
    public String getName() {
        return "libraries";
    }
}
