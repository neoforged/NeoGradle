package net.neoforged.gradle.common.extensions.dependency.creation;

import org.gradle.api.Task;
import org.gradle.api.artifacts.Dependency;

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
    Dependency from(Task task);
}
