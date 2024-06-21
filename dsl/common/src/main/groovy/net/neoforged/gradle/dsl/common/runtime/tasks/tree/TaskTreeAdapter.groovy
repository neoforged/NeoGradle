package net.neoforged.gradle.dsl.common.runtime.tasks.tree

import groovy.transform.CompileStatic
import net.minecraftforge.gdi.annotations.DefaultMethods
import net.neoforged.gradle.dsl.common.runtime.definition.LegacyDefinition
import net.neoforged.gradle.dsl.common.runtime.tasks.Runtime
import net.neoforged.gradle.dsl.common.tasks.WithOutput
import net.neoforged.gradle.dsl.common.util.GameArtifact
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable

import java.util.function.Consumer

/**
 * Defines a callback which is invoked to handle modifications to an MCP task tree.
 */
@DefaultMethods
@CompileStatic
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
    TaskProvider<? extends Runtime> adapt(final LegacyDefinition<?> definition, final Provider<? extends WithOutput> previousTasksOutput, final File runtimeWorkspace, final Map<GameArtifact, TaskProvider<? extends WithOutput>> gameArtifacts, final Map<String, String> mappingVersionData, final Consumer<TaskProvider<? extends Runtime>> dependentTaskConfigurationHandler);

    /**
     * Runs the given task adapter after the current one.
     * Implicitly chaining the build output of this adapters task as the input for the given adapters task.
     * Automatically configures the task tree compileDependencies.
     *
     * @param after The task tree adapter to run afterwards.
     * @return The combined task tree adapter.
     */
    @NotNull
    default TaskTreeAdapter andThen(final TaskTreeAdapter after) {
        Objects.requireNonNull(after);
        return new AndTaskTreeAdapter(this, after);
    }
}
