package net.neoforged.gradle.dsl.common.runtime.spec;

import net.neoforged.gradle.dsl.common.runtime.definition.Outputs;
import net.neoforged.gradle.dsl.common.runtime.definition.TaskHandler;
import org.gradle.api.artifacts.Configuration;

/**
 * A builder for a task tree.
 * Produces a {@link TaskHandler} that can be used to manage tasks for the runtime.
 */
public interface TaskTreeBuilder {

    /**
     * @return The built task tree.
     */
    BuildResult build();

    /**
     * A record representing the result of building a task tree.
     *
     * @param compileDependencies The compile dependencies of the task tree.
     * @param runtimeDependencies The runtime dependencies of the task tree.
     * @param handler The task handler for the task tree.
     * @param outputs The outputs of the task tree.
     */
    record BuildResult(
            Configuration compileDependencies,
            Configuration runtimeDependencies,
            TaskHandler handler,
            Outputs outputs
    ) {}
}
