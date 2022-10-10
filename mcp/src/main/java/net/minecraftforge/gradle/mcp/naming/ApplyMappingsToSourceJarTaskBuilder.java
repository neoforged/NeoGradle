package net.minecraftforge.gradle.mcp.naming;

import net.minecraftforge.gradle.mcp.runtime.spec.McpRuntimeSpec;
import net.minecraftforge.gradle.mcp.runtime.tasks.IMcpRuntimeTask;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Factory interface which can construct a new task provider for a source jar mapping.
 */
@FunctionalInterface
public interface ApplyMappingsToSourceJarTaskBuilder {

    /**
     * Invoked to construct a new task provider for a source jar mapping from the given context.
     *
     * @param context The context.
     * @return The task provider.
     */
    @NotNull TaskProvider<? extends IMcpRuntimeTask> build(RenamingTaskBuildingContext context);
}
