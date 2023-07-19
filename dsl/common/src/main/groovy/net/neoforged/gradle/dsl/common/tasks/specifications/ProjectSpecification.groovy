package net.neoforged.gradle.dsl.common.tasks.specifications

import groovy.transform.CompileStatic
import net.minecraftforge.gdi.annotations.ProjectGetter
import org.gradle.api.Project
import org.gradle.api.tasks.Internal

/**
 * Defines an object with a specification that holds a project
 */
@CompileStatic
interface ProjectSpecification {

    /**
     * The project for the object.
     *
     * @return The project for the task.
     */
    @Internal
    @ProjectGetter
    abstract Project getProject();
}