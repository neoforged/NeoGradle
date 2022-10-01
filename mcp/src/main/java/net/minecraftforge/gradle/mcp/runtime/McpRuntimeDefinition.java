package net.minecraftforge.gradle.mcp.runtime;

import com.google.common.collect.Iterators;
import net.minecraftforge.gradle.mcp.configuration.McpConfigConfigurationSpecV2;
import net.minecraftforge.gradle.mcp.runtime.spec.McpRuntimeSpec;
import net.minecraftforge.gradle.mcp.runtime.tasks.McpRuntime;
import net.minecraftforge.gradle.mcp.util.McpConfigConstants;
import net.minecraftforge.gradle.mcp.util.McpRuntimeUtils;
import org.gradle.api.Project;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Objects;

/**
 * Represents a configured and registered runtime for Mcp.
 *
 * @param project                 The project that the runtime is registered to.
 * @param spec                    The spec that build the runtime.
 * @param tasks                   The tasks that were build from the spec.
 * @param unpackedMcpZipDirectory The unpacked mcp zip location.
 * @param mcpConfig               The mcp config.
 */
public record McpRuntimeDefinition(
        @Input McpRuntimeSpec spec,
        @Nested LinkedHashMap<String, TaskProvider<? extends McpRuntime>> tasks,
        @Input File unpackedMcpZipDirectory,
        @Nested McpConfigConfigurationSpecV2 mcpConfig
) {

    /**
     * @return The last task in the task chain.
     */
    @NotNull
    public TaskProvider<? extends McpRuntime> lastTask() {
        return Iterators.getLast(tasks.values().iterator());
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
    public TaskProvider<? extends McpRuntime> task(String name) {
        final String taskName = McpRuntimeUtils.buildTaskName(this, name);
        if (!tasks.containsKey(taskName)) {
            throw new IllegalArgumentException("No task with name " + name + " found in runtime " + spec.name());
        }

        return tasks.get(taskName);
    }

    public TaskProvider<? extends McpRuntime> rawJarTask() {
        return switch (spec().side()) {
            case "client" -> task(McpConfigConstants.Steps.Outputs.CLIENT_RAW);
            case "server" -> task(McpConfigConstants.Steps.Outputs.SERVER_RAW);
            case "joined" -> task(McpConfigConstants.Steps.Outputs.JOINED_RAW);
            default -> throw new IllegalStateException("Unexpected side: " + spec().side());
        };
    }

    /**
     * Provides lazy access to the mcp config tsrg mapping file for this runtime.
     *
     * @return The mcp config tsrg mapping file.
     */
    @NotNull
    public Provider<File> getTsrgMappingsFile() {
        return spec.project().provider(() -> new File(unpackedMcpZipDirectory(), Objects.requireNonNull(mcpConfig().getData(McpConfigConstants.Data.MAPPINGS))));
    }
}
