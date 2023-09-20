package net.neoforged.gradle.dsl.common.tasks

import groovy.transform.CompileStatic
import net.neoforged.gradle.dsl.common.tasks.specifications.JavaVersionSpecification
import org.gradle.api.Task

/**
 * Defines a task with a java version property.
 */
@CompileStatic
trait WithJavaVersion extends WithProject implements WithOperations, JavaVersionSpecification {
}
