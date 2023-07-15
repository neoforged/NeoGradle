package net.neoforged.gradle.dsl.common.tasks

import groovy.transform.CompileStatic
import net.neoforged.gradle.dsl.common.tasks.specifications.ProjectSpecification
import org.gradle.api.Task

/**
 * Interface that indicates that a given task has a project associated with it.
 */
@CompileStatic
trait WithProject implements Task, ProjectSpecification {
}