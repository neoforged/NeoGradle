package net.minecraftforge.gradle.dsl.base.util

import groovy.transform.CompileStatic
import net.minecraftforge.gradle.dsl.annotations.ProjectGetter
import org.gradle.api.Project

/**
 * Defines a DSL object which which is part of a project structure.
 * Allows access to the project it belongs to.
 */
@CompileStatic
interface ProjectAssociatedDSLElement {

    /**
     * @returns The project that this object belongs to.
     */
    @ProjectGetter
    Project getProject();
}