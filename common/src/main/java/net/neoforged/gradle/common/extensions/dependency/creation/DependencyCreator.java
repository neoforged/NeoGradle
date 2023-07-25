package net.neoforged.gradle.common.extensions.dependency.creation;

import org.gradle.api.Task;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.tasks.TaskProvider;

/**
 * Defines logic to create dependencies from different sources.
 */
public interface DependencyCreator {

    /**
     * Creates a dependency from a task output.
     *
     * @param task The task.
     * @return The dependency.
     */
    Dependency from(TaskProvider<? extends Task> task);
}
