package net.minecraftforge.gradle.dsl.common.tasks

import groovy.transform.CompileStatic
import net.minecraftforge.gradle.dsl.annotations.ProjectGetter
import org.gradle.api.Project
import org.gradle.api.Task

/**
 * Interface that indicates that a given task has a project associated with it.
 */
@CompileStatic
trait WithProject implements Task {

    /**
     * The project for the task.
     * Handled by the default task system, overridden so that the project getter can be marked.
     *
     * @return The project for the task.
     */
    @ProjectGetter
    @Override
    abstract Project getProject();
}