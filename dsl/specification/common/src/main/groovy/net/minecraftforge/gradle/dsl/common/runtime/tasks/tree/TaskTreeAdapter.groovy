package net.minecraftforge.gradle.dsl.common.runtime.tasks.tree


import net.minecraftforge.gradle.dsl.common.runtime.definition.Definition
import net.minecraftforge.gradle.dsl.common.runtime.spec.Specification
import net.minecraftforge.gradle.dsl.common.runtime.tasks.Runtime
import net.minecraftforge.gradle.dsl.common.tasks.WithOutput
import net.minecraftforge.gradle.dsl.common.util.GameArtifact
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable

import java.util.function.Consumer

/**
 * Defines a callback which is invoked to handle modifications to an MCP task tree.
 */
@FunctionalInterface
interface TaskTreeAdapter {

    /**
     * Invoked to get a task which is run after the taskOutputs of which the output is given.
     * The invoker is responsible for registering the task to the project, which is retrievable via the specifications project.
     *
     * @param definition The runtime definition to build a task for.
     * @param previousTasksOutput The previous task build output.
     * @return The task to run.
     */
    @Nullable
    TaskProvider<? extends Runtime> adapt(final Definition<?> definition, final Provider<? extends WithOutput> previousTasksOutput, final File runtimeWorkspace, final Map<GameArtifact, TaskProvider<? extends WithOutput>> gameArtifacts, final Map<String, String> mappingVersionData, final Consumer<TaskProvider<? extends Runtime>> dependentTaskConfigurationHandler);

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

        return new TaskTreeAdapter() {
            @Override
            TaskProvider<? extends Runtime> adapt(Definition<? extends Specification> definition, Provider<? extends WithOutput> previousTasksOutput, File runtimeWorkspace, Map<GameArtifact, TaskProvider<? extends WithOutput>> gameArtifacts, Map<String, String> mappingVersionData, Consumer<TaskProvider<? extends Runtime>> dependentTaskConfigurationHandler) {
                final TaskProvider<? extends Runtime> currentAdapted = TaskTreeAdapter.this.adapt(definition, previousTasksOutput, runtimeWorkspace, gameArtifacts, mappingVersionData, dependentTaskConfigurationHandler);

                if (currentAdapted != null)
                    dependentTaskConfigurationHandler.accept(currentAdapted);

                final TaskProvider<? extends Runtime> afterAdapted = after.adapt(definition, currentAdapted, runtimeWorkspace, gameArtifacts, mappingVersionData, dependentTaskConfigurationHandler);

                if (currentAdapted != null && afterAdapted != null)
                    afterAdapted.configure(task -> task.dependsOn(currentAdapted));

                return afterAdapted;
            }
        }
    }

}
