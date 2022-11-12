package net.minecraftforge.gradle.vanilla.runtime.steps;

import net.minecraftforge.gradle.common.runtime.tasks.IRuntimeTask;
import net.minecraftforge.gradle.common.util.GameArtifact;
import net.minecraftforge.gradle.vanilla.runtime.spec.VanillaRuntimeSpec;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.internal.impldep.com.google.api.client.json.JsonPolymorphicTypeMap;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public interface IStep {

    TaskProvider<? extends IRuntimeTask> buildTask(VanillaRuntimeSpec spec, TaskProvider<? extends IRuntimeTask> inputProvidingTask, @NotNull File minecraftCache, @NotNull Map<String, TaskProvider<? extends IRuntimeTask>> pipelineTasks, @NotNull Map<GameArtifact, File> gameArtifacts, @NotNull Map<GameArtifact, TaskProvider<? extends IRuntimeTask>> gameArtifactTasks, @NotNull Consumer<TaskProvider<? extends IRuntimeTask>> additionalTaskConfigurator);

    String getName();
}
