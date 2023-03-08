package net.minecraftforge.gradle.dsl.base.util

import groovy.transform.CompileStatic
import net.minecraftforge.gdi.annotations.ProjectGetter
import org.gradle.api.Project
import org.gradle.api.tasks.Internal

/**
 * Defines a DSL object which which is part of a project structure.
 * Allows access to the project it belongs to.
 */
@CompileStatic
interface ProjectAssociatedDSLElement {

    /**
     * @return The project that this object belongs to.
     */
    @Internal
    @ProjectGetter
    Project getProject();
}