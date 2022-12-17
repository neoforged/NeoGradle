package net.minecraftforge.gradle.common.runtime.spec;

import net.minecraftforge.gradle.common.runtime.tasks.IRuntimeTask;
import net.minecraftforge.gradle.common.tasks.ITaskWithOutput;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Defines a callback which is invoked to handle modifications to an MCP task tree.
 */
@FunctionalInterface
public interface TaskTreeAdapter {

    /**
     * Invoked to get a task which is run after the taskOutputs of which the output is given.
     * The invoker is responsible for registering the task to the project, which is retrievable via {@link  CommonRuntimeSpec#project()}.
     *
     * @param spec The runtime spec to build a task for.
     * @param previousTasksOutput The previous task build output.
     * @return The task to run.
     */
    @NotNull
    TaskProvider<? extends IRuntimeTask> adapt(final CommonRuntimeSpec spec, final Provider<? extends ITaskWithOutput> previousTasksOutput, final Consumer<TaskProvider<? extends IRuntimeTask>> dependentTaskConfigurationHandler);

    /**
     * Runs the given task adapter after the current one.
     * Implicitly chaining the build output of this adapters task as the input for the given adapters task.
     * Automatically configures the task tree dependencies.
     *
     * @param after The task tree adapter to run afterwards.
     * @return The combined task tree adapter.
     */
    @NotNull
    default TaskTreeAdapter andThen(final TaskTreeAdapter after) {
        Objects.requireNonNull(after);
        return (spec, previousTaskOutput, dependentTaskConfigurationHandler) -> {
            final TaskProvider<? extends IRuntimeTask> currentAdapted = TaskTreeAdapter.this.adapt(spec, previousTaskOutput, dependentTaskConfigurationHandler);
            dependentTaskConfigurationHandler.accept(currentAdapted);

            final TaskProvider<? extends IRuntimeTask> afterAdapted = after.adapt(spec, currentAdapted, dependentTaskConfigurationHandler);
            afterAdapted.configure(task -> task.dependsOn(currentAdapted));

            return afterAdapted;
        };
    }

}
