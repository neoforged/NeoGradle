package net.minecraftforge.gradle.common.runtime.naming;

import net.minecraftforge.gradle.mcp.configuration.McpConfigConfigurationSpecV2;
import net.minecraftforge.gradle.mcp.runtime.spec.McpRuntimeSpec;
import net.minecraftforge.gradle.common.util.GameArtifact;
import net.minecraftforge.gradle.mcp.runtime.tasks.IMcpRuntimeTask;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Defines the contextual data that is available to {@link NamingChannelProvider naming channel providers} when they
 * are requested to build a new task that remaps the source jar that is provided via {@link #taskOutputToModify()}
 */
public final class RenamingTaskBuildingContext {
        private final @NotNull McpRuntimeSpec spec;
        private final @NotNull File minecraftCache;
        private final @NotNull Map<String, TaskProvider<? extends IMcpRuntimeTask>> pipelineTasks;
        private final @NotNull NamingChannelProvider namingChannelProvider;
        private final @NotNull Map<String, String> mappingVersionData;
        private final @NotNull TaskProvider<? extends IMcpRuntimeTask> taskOutputToModify;
        private final @NotNull File unpackedMcpZipDirectory;
        private final @NotNull McpConfigConfigurationSpecV2 mcpConfig;
        private final @NotNull Map<GameArtifact, File> gameArtifacts;
        private final @NotNull Map<GameArtifact, TaskProvider<? extends IMcpRuntimeTask>> gameArtifactTasks;
        private final @NotNull Set<TaskProvider<? extends IMcpRuntimeTask>> additionalRuntimeTasks;

        /**
         *
         */
        public RenamingTaskBuildingContext(
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
                this.spec = spec;
                this.minecraftCache = minecraftCache;
                this.pipelineTasks = pipelineTasks;
                this.namingChannelProvider = namingChannelProvider;
                this.mappingVersionData = mappingVersionData;
                this.taskOutputToModify = taskOutputToModify;
                this.unpackedMcpZipDirectory = unpackedMcpZipDirectory;
                this.mcpConfig = mcpConfig;
                this.gameArtifacts = gameArtifacts;
                this.gameArtifactTasks = gameArtifactTasks;
                this.additionalRuntimeTasks = additionalRuntimeTasks;
        }

        public RenamingTaskBuildingContext(@NotNull McpRuntimeSpec spec, @NotNull File minecraftCache, @NotNull Map<String, TaskProvider<? extends IMcpRuntimeTask>> pipelineTasks, @NotNull NamingChannelProvider namingChannelProvider, @NotNull Map<String, String> mappingVersionData, @NotNull TaskProvider<? extends IMcpRuntimeTask> taskOutputToModify, @NotNull File unpackedMcpZipDirectory, @NotNull McpConfigConfigurationSpecV2 mcpConfig, @NotNull Map<GameArtifact, File> gameArtifacts, @NotNull Map<GameArtifact, TaskProvider<? extends IMcpRuntimeTask>> gameArtifactTasks) {
                this(spec, minecraftCache, pipelineTasks, namingChannelProvider, mappingVersionData, taskOutputToModify, unpackedMcpZipDirectory, mcpConfig, gameArtifacts, gameArtifactTasks, new HashSet<>());
        }

        public void withAdditionalRuntimeTask(final TaskProvider<? extends IMcpRuntimeTask> task) {
                additionalRuntimeTasks.add(task);
        }

        public TaskProvider<? extends IMcpRuntimeTask> gameArtifactTask(final GameArtifact artifact) {
                return gameArtifactTasks.computeIfAbsent(artifact, a -> {
                        throw new IllegalStateException(String.format("No task found for game artifact %s", a));
                });
        }

        public @NotNull McpRuntimeSpec spec() {
                return spec;
        }

        public @NotNull File minecraftCache() {
                return minecraftCache;
        }

        public @NotNull Map<String, TaskProvider<? extends IMcpRuntimeTask>> pipelineTasks() {
                return pipelineTasks;
        }

        public @NotNull NamingChannelProvider namingChannelProvider() {
                return namingChannelProvider;
        }

        public @NotNull Map<String, String> mappingVersionData() {
                return mappingVersionData;
        }

        public @NotNull TaskProvider<? extends IMcpRuntimeTask> taskOutputToModify() {
                return taskOutputToModify;
        }

        public @NotNull File unpackedMcpZipDirectory() {
                return unpackedMcpZipDirectory;
        }

        public @NotNull McpConfigConfigurationSpecV2 mcpConfig() {
                return mcpConfig;
        }

        public @NotNull Map<GameArtifact, File> gameArtifacts() {
                return gameArtifacts;
        }

        public @NotNull Map<GameArtifact, TaskProvider<? extends IMcpRuntimeTask>> gameArtifactTasks() {
                return gameArtifactTasks;
        }

        public @NotNull Set<TaskProvider<? extends IMcpRuntimeTask>> additionalRuntimeTasks() {
                return additionalRuntimeTasks;
        }

        @Override
        public boolean equals(Object obj) {
                if (obj == this) return true;
                if (obj == null || obj.getClass() != this.getClass()) return false;
                final RenamingTaskBuildingContext that = (RenamingTaskBuildingContext) obj;
                return Objects.equals(this.spec, that.spec) &&
                        Objects.equals(this.minecraftCache, that.minecraftCache) &&
                        Objects.equals(this.pipelineTasks, that.pipelineTasks) &&
                        Objects.equals(this.namingChannelProvider, that.namingChannelProvider) &&
                        Objects.equals(this.mappingVersionData, that.mappingVersionData) &&
                        Objects.equals(this.taskOutputToModify, that.taskOutputToModify) &&
                        Objects.equals(this.unpackedMcpZipDirectory, that.unpackedMcpZipDirectory) &&
                        Objects.equals(this.mcpConfig, that.mcpConfig) &&
                        Objects.equals(this.gameArtifacts, that.gameArtifacts) &&
                        Objects.equals(this.gameArtifactTasks, that.gameArtifactTasks) &&
                        Objects.equals(this.additionalRuntimeTasks, that.additionalRuntimeTasks);
        }

        @Override
        public int hashCode() {
                return Objects.hash(spec, minecraftCache, pipelineTasks, namingChannelProvider, mappingVersionData, taskOutputToModify, unpackedMcpZipDirectory, mcpConfig, gameArtifacts, gameArtifactTasks, additionalRuntimeTasks);
        }

        @Override
        public String toString() {
                return "RenamingTaskBuildingContext[" +
                        "spec=" + spec + ", " +
                        "minecraftCache=" + minecraftCache + ", " +
                        "pipelineTasks=" + pipelineTasks + ", " +
                        "namingChannelProvider=" + namingChannelProvider + ", " +
                        "mappingVersionData=" + mappingVersionData + ", " +
                        "taskOutputToModify=" + taskOutputToModify + ", " +
                        "unpackedMcpZipDirectory=" + unpackedMcpZipDirectory + ", " +
                        "mcpConfig=" + mcpConfig + ", " +
                        "gameArtifacts=" + gameArtifacts + ", " +
                        "gameArtifactTasks=" + gameArtifactTasks + ", " +
                        "additionalRuntimeTasks=" + additionalRuntimeTasks + ']';
        }

}
