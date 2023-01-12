package net.minecraftforge.gradle.dsl.common.tasks

import groovy.transform.CompileStatic
import net.minecraftforge.gradle.dsl.annotations.DefaultMethods
import net.minecraftforge.gradle.dsl.common.tasks.specifications.OutputSpecification;
import org.gradle.api.Task

/**
 * Interface that indicates that a given task has an output which can be further processed.
 */
@CompileStatic
@DefaultMethods
trait WithOutput implements Task, WithWorkspace, WithProject, OutputSpecification {

}
