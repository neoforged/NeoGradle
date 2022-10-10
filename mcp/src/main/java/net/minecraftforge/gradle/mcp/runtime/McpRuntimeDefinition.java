package net.minecraftforge.gradle.mcp.runtime;

import com.google.common.collect.Iterators;
import net.minecraftforge.gradle.common.tasks.ITaskWithOutput;
import net.minecraftforge.gradle.common.util.GameArtifact;
import net.minecraftforge.gradle.mcp.configuration.McpConfigConfigurationSpecV2;
import net.minecraftforge.gradle.mcp.runtime.spec.McpRuntimeSpec;
import net.minecraftforge.gradle.mcp.runtime.tasks.IMcpRuntimeTask;
import net.minecraftforge.gradle.mcp.util.McpRuntimeUtils;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents a configured and registered runtime for Mcp.
 *
 * @param spec                               The spec that build the runtime.
 * @param tasks                              The tasks that were build from the spec.
 * @param unpackedMcpZipDirectory            The unpacked mcp zip location.
 * @param mcpConfig                          The mcp config.
 * @param minecraftDependenciesConfiguration
 */
public record McpRuntimeDefinition(
        McpRuntimeSpec spec,
        LinkedHashMap<String, TaskProvider<? extends IMcpRuntimeTask>> tasks,
        File unpackedMcpZipDirectory,
        McpConfigConfigurationSpecV2 mcpConfig,
        TaskProvider<? extends ITaskWithOutput> sourceJarTask,
        TaskProvider<? extends ITaskWithOutput> rawJarTask,
        Map<GameArtifact, TaskProvider<? extends IMcpRuntimeTask>> gameArtifactProvidingTasks,
        Configuration minecraftDependenciesConfiguration) {

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
        if (!tasks.containsKey(taskName)) {
            throw new IllegalArgumentException("No task with name " + name + " found in runtime " + spec.name());
        }

        return tasks.get(taskName);
    }

    /**
     * Returns the task which produces the raw jar used for compiling against.
     *
     * @return The raw jar producing tasks.
     */
    public TaskProvider<? extends ITaskWithOutput> rawJarTask() {
        return Iterators.getLast(tasks.values().iterator());
    }
}
