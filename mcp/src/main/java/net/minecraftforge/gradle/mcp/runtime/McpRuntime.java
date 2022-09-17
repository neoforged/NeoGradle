package net.minecraftforge.gradle.mcp.runtime;

import com.google.common.collect.Iterators;
import net.minecraftforge.gradle.mcp.runtime.spec.McpRuntimeSpec;
import net.minecraftforge.gradle.mcp.runtime.tasks.McpRuntimeTask;
import org.gradle.api.tasks.Input;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;

/**
 * Represents a configured and registered runtime for Mcp.
 * @param spec The spec that build the runtime.
 * @param tasks The tasks that were build from the spec.
 */
public record McpRuntime(@Input McpRuntimeSpec spec, @Input LinkedHashMap<String, McpRuntimeTask> tasks) {

    /**
     * @return The last task in the task chain.
     */
    @NotNull
    public McpRuntimeTask lastTask() {
        return Iterators.getLast(tasks.values().iterator());
    }
}
