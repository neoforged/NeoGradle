package net.minecraftforge.gradle.dsl.base.util

import groovy.transform.CompileStatic
import org.gradle.api.tasks.Input

/**
 * Defines a DSL object which has a name.
 */
@CompileStatic
interface NamedDSLElement {

    /**
     * @return The name of the project.
     */
    @Input
    String getName();
}