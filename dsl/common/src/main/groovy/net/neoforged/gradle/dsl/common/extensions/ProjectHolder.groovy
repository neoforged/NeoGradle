package net.neoforged.gradle.dsl.common.extensions

import groovy.transform.CompileStatic
import net.neoforged.gradle.dsl.common.tasks.specifications.ProjectSpecification

/**
 * An extension used to attach projects to {@link org.gradle.api.plugins.ExtensionAware} objects.
 */
@CompileStatic
interface ProjectHolder extends ProjectSpecification {
}