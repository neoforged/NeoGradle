package net.neoforged.gradle.dsl.common.runtime.spec;

import net.neoforged.gradle.dsl.common.runtime.definition.TaskHandler;
import org.gradle.api.artifacts.Configuration;

/**
 * A builder for a task tree.
 * Produces a {@link TaskHandler} that can be used to manage tasks for the runtime.
 */
public interface TaskTreeBuilder {

    /**
     * Builds a task tree from the given specification.
     *
     * @param specification The specification to build the task tree from.
     * @return The task tree.
     */
    BuildResult build(Specification specification);

    /**
     * A record representing the result of building a task tree.
     *
     * @param dependencies The dependencies of the task tree.
     * @param handler The task handler for the task tree.
     */
    record BuildResult(Configuration dependencies, TaskHandler handler) {}
}
