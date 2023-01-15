package net.minecraftforge.gradle.dsl.common.extensions

import groovy.transform.CompileStatic
import net.minecraftforge.gradle.dsl.common.tasks.specifications.ProjectSpecification

/**
 * An extension used to attach projects to {@link org.gradle.api.plugins.ExtensionAware} objects.
 */
@CompileStatic
interface ProjectHolder extends ProjectSpecification {
}