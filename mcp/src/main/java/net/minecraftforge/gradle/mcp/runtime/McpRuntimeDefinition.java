package net.minecraftforge.gradle.mcp.runtime;

import com.google.common.collect.Iterators;
import net.minecraftforge.gradle.mcp.configuration.McpConfigConfigurationSpecV2;
import net.minecraftforge.gradle.mcp.runtime.spec.McpRuntimeSpec;
import net.minecraftforge.gradle.common.tasks.ITaskWithOutput;
import net.minecraftforge.gradle.common.util.GameArtifact;
import net.minecraftforge.gradle.mcp.util.McpRuntimeUtils;
import net.minecraftforge.gradle.mcp.runtime.tasks.IMcpRuntimeTask;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a configured and registered runtime for Mcp.
 */
public final class McpRuntimeDefinition {
    private final McpRuntimeSpec spec;
    private final LinkedHashMap<String, TaskProvider<? extends IMcpRuntimeTask>> taskOutputs;
    private final File unpackedMcpZipDirectory;
    private final McpConfigConfigurationSpecV2 mcpConfig;
    private final TaskProvider<? extends ITaskWithOutput> sourceJarTask;
    private final TaskProvider<? extends ITaskWithOutput> rawJarTask;
    private final Map<GameArtifact, TaskProvider<? extends IMcpRuntimeTask>> gameArtifactProvidingTasks;
    private final Configuration minecraftDependenciesConfiguration;

    /**
     * @param spec                               The spec that build the runtime.
     * @param taskOutputs                        The taskOutputs that were build from the spec.
     * @param unpackedMcpZipDirectory            The unpacked mcp zip location.
     * @param mcpConfig                          The mcp config.
     * @param minecraftDependenciesConfiguration
     */
    public McpRuntimeDefinition(
            McpRuntimeSpec spec,
            LinkedHashMap<String, TaskProvider<? extends IMcpRuntimeTask>> taskOutputs,
            File unpackedMcpZipDirectory,
            McpConfigConfigurationSpecV2 mcpConfig,
            TaskProvider<? extends ITaskWithOutput> sourceJarTask,
            TaskProvider<? extends ITaskWithOutput> rawJarTask,

            Map<GameArtifact, TaskProvider<? extends IMcpRuntimeTask>> gameArtifactProvidingTasks,
            Configuration minecraftDependenciesConfiguration) {
        this.spec = spec;
        this.taskOutputs = taskOutputs;
        this.unpackedMcpZipDirectory = unpackedMcpZipDirectory;
        this.mcpConfig = mcpConfig;
        this.sourceJarTask = sourceJarTask;
        this.rawJarTask = rawJarTask;
        this.gameArtifactProvidingTasks = gameArtifactProvidingTasks;
        this.minecraftDependenciesConfiguration = minecraftDependenciesConfiguration;
    }

    /**
     * Returns the runtimes task with the given name.
     * The given name is prefixed with the name of the runtime, if needed.
     * Invoking this method with the name of a task that is not part of the runtime will result in an {@link IllegalArgumentException exception}.
     *
     * @param name The name of the task to get.
     * @return The named task.
     */
    @NotNull
    public TaskProvider<? extends ITaskWithOutput> task(String name) {
        final String taskName = McpRuntimeUtils.buildTaskName(this, name);
        if (!taskOutputs.containsKey(taskName)) {
            throw new IllegalArgumentException("No task with name " + name + " found in runtime " + spec.name());
        }

        return taskOutputs.get(taskName);
    }

    /**
     * Returns the task which produces the raw jar used for compiling against.
     *
     * @return The raw jar producing taskOutputs.
     */
    public TaskProvider<? extends ITaskWithOutput> rawJarTask() {
        return Iterators.getLast(taskOutputs.values().iterator());
    }

    public McpRuntimeSpec spec() {
        return spec;
    }

    public LinkedHashMap<String, TaskProvider<? extends IMcpRuntimeTask>> taskOutputs() {
        return taskOutputs;
    }

    public File unpackedMcpZipDirectory() {
        return unpackedMcpZipDirectory;
    }

    public McpConfigConfigurationSpecV2 mcpConfig() {
        return mcpConfig;
    }

    public TaskProvider<? extends ITaskWithOutput> sourceJarTask() {
        return sourceJarTask;
    }

    public Map<GameArtifact, TaskProvider<? extends IMcpRuntimeTask>> gameArtifactProvidingTasks() {
        return gameArtifactProvidingTasks;
    }

    public Configuration minecraftDependenciesConfiguration() {
        return minecraftDependenciesConfiguration;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        final McpRuntimeDefinition that = (McpRuntimeDefinition) obj;
        return Objects.equals(this.spec, that.spec) &&
                Objects.equals(this.taskOutputs, that.taskOutputs) &&
                Objects.equals(this.unpackedMcpZipDirectory, that.unpackedMcpZipDirectory) &&
                Objects.equals(this.mcpConfig, that.mcpConfig) &&
                Objects.equals(this.sourceJarTask, that.sourceJarTask) &&
                Objects.equals(this.rawJarTask, that.rawJarTask) &&
                Objects.equals(this.gameArtifactProvidingTasks, that.gameArtifactProvidingTasks) &&
                Objects.equals(this.minecraftDependenciesConfiguration, that.minecraftDependenciesConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(spec, taskOutputs, unpackedMcpZipDirectory, mcpConfig, sourceJarTask, rawJarTask, gameArtifactProvidingTasks, minecraftDependenciesConfiguration);
    }

    @Override
    public String toString() {
        return "McpRuntimeDefinition[" +
                "spec=" + spec + ", " +
                "taskOutputs=" + taskOutputs + ", " +
                "unpackedMcpZipDirectory=" + unpackedMcpZipDirectory + ", " +
                "mcpConfig=" + mcpConfig + ", " +
                "sourceJarTask=" + sourceJarTask + ", " +
                "rawJarTask=" + rawJarTask + ", " +
                "gameArtifactProvidingTasks=" + gameArtifactProvidingTasks + ", " +
                "minecraftDependenciesConfiguration=" + minecraftDependenciesConfiguration + ']';
    }

}
