package net.minecraftforge.gradle.mcp.naming;

import net.minecraftforge.gradle.common.util.GameArtifact;
import net.minecraftforge.gradle.mcp.configuration.McpConfigConfigurationSpecV2;
import net.minecraftforge.gradle.mcp.runtime.spec.McpRuntimeSpec;
import net.minecraftforge.gradle.mcp.runtime.tasks.IMcpRuntimeTask;
import org.gradle.api.Task;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Defines the contextual data that is available to {@link NamingChannelProvider naming channel providers} when they
 * are requested to build a new task that remaps the source jar that is provided via {@link #taskOutputToModify()}
 */
public record RenamingTaskBuildingContext(
        @NotNull McpRuntimeSpec spec,
        @NotNull File minecraftCache,
        @NotNull Map<String, TaskProvider<? extends IMcpRuntimeTask>> pipelineTasks,
        @NotNull NamingChannelProvider namingChannelProvider,
        @NotNull Map<String, String> mappingVersionData,
        @NotNull TaskProvider<? extends IMcpRuntimeTask> taskOutputToModify,
        @NotNull File unpackedMcpZipDirectory,
        @NotNull McpConfigConfigurationSpecV2 mcpConfig,
        @NotNull Map<GameArtifact, File> gameArtifacts,
        @NotNull Map<GameArtifact, TaskProvider<? extends IMcpRuntimeTask>> gameArtifactTasks,
        @NotNull Set<TaskProvider<? extends IMcpRuntimeTask>> additionalRuntimeTasks
        ) {

        public RenamingTaskBuildingContext(@NotNull McpRuntimeSpec spec, @NotNull File minecraftCache, @NotNull Map<String, TaskProvider<? extends IMcpRuntimeTask>> pipelineTasks, @NotNull NamingChannelProvider namingChannelProvider, @NotNull Map<String, String> mappingVersionData, @NotNull TaskProvider<? extends IMcpRuntimeTask> taskOutputToModify, @NotNull File unpackedMcpZipDirectory, @NotNull McpConfigConfigurationSpecV2 mcpConfig, @NotNull Map<GameArtifact, File> gameArtifacts, @NotNull Map<GameArtifact, TaskProvider<? extends IMcpRuntimeTask>> gameArtifactTasks) {
                this(spec, minecraftCache, pipelineTasks, namingChannelProvider, mappingVersionData, taskOutputToModify, unpackedMcpZipDirectory, mcpConfig, gameArtifacts, gameArtifactTasks, new HashSet<>());
        }

        public void withAdditionalRuntimeTask(final TaskProvider<? extends IMcpRuntimeTask> task) {
                additionalRuntimeTasks.add(task);
        }

        public TaskProvider<? extends IMcpRuntimeTask> gameArtifactTask(final GameArtifact artifact) {
                return gameArtifactTasks.computeIfAbsent(artifact, a -> {
                        throw new IllegalStateException("No task found for game artifact %s".formatted(a));
                });
        }
}
