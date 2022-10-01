package net.minecraftforge.gradle.mcp.runtime.spec;

import net.minecraftforge.gradle.mcp.runtime.tasks.McpRuntime;
import org.gradle.api.provider.Provider;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Objects;

/**
 * Defines a callback which is invoked to handle modifications to an MCP task tree.
 */
@FunctionalInterface
public interface TaskTreeAdapter {

    /**
     * Invoked to get a task which is run after the tasks of which the output is given.
     * The invoker is responsible for registering the task to the project, which is retrievable via {@link  McpRuntimeSpec#project()}.
     *
     * @param spec The runtime spec to build a task for.
     * @param previousTasksOutput The previous task build output.
     * @return The task to run.
     */
    @NotNull
    McpRuntime adapt(final McpRuntimeSpec spec, final Provider<File> previousTasksOutput);

    /**
     * Runs the given task adapter after the current one.
     * Implicitly chaining the build output of this adapters task as the input for the given adapters task.
     *
     * @param after The task tree adapter to run afterwards.
     * @return The combined task tree adapter.
     */
    @NotNull
    default TaskTreeAdapter andThen(final TaskTreeAdapter after) {
        Objects.requireNonNull(after);
        return (McpRuntimeSpec spec, Provider<File> previousTaskOutput) -> after.adapt(spec, adapt(spec, previousTaskOutput).getOutput().getAsFile());
    }

}
