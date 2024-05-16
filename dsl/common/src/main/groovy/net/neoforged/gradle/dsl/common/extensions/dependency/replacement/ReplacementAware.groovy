package net.neoforged.gradle.dsl.common.extensions.dependency.replacement

import net.neoforged.gradle.dsl.common.tasks.WithOutput
import org.gradle.api.tasks.TaskProvider

/**
 * Defines an object that is aware of dynamic dependency replacement.
 */
interface ReplacementAware {

    /**
     * Called when tasks are created for dependency replacement.
     *
     * @param copiesRawJar The task that copies the raw jar.
     * @param copiesMappedJar The task that copies the mapped jar.
     */
    void onTasksCreated(
            TaskProvider<? extends WithOutput> copiesRawJar,
            TaskProvider<? extends WithOutput> copiesMappedJar
    );
}
