package net.minecraftforge.gradle.vanilla.runtime.steps;

import net.minecraftforge.gradle.common.runtime.tasks.IRuntimeTask;
import net.minecraftforge.gradle.common.tasks.ITaskWithOutput;
import net.minecraftforge.gradle.common.util.GameArtifact;
import net.minecraftforge.gradle.vanilla.runtime.VanillaRuntimeDefinition;
import net.minecraftforge.gradle.vanilla.runtime.spec.VanillaRuntimeSpec;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.internal.impldep.com.google.api.client.json.JsonPolymorphicTypeMap;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public interface IStep {

    TaskProvider<? extends IRuntimeTask> buildTask(VanillaRuntimeDefinition definition, TaskProvider<? extends ITaskWithOutput> inputProvidingTask, @NotNull File minecraftCache, @NotNull Map<String, TaskProvider<? extends ITaskWithOutput>> pipelineTasks, @NotNull Map<GameArtifact, File> gameArtifacts, @NotNull Map<GameArtifact, TaskProvider<? extends ITaskWithOutput>> gameArtifactTasks, @NotNull Consumer<TaskProvider<? extends IRuntimeTask>> additionalTaskConfigurator);

    String getName();
}
