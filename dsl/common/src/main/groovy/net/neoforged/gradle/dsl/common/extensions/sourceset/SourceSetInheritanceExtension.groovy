package net.neoforged.gradle.dsl.common.extensions.sourceset

import groovy.transform.CompileStatic
import net.minecraftforge.gdi.BaseDSLElement
import org.gradle.api.tasks.SourceSet

/**
 * Represents an extension on a source set that allows for inheritance of source sets.
 */
@CompileStatic
interface SourceSetInheritanceExtension extends BaseDSLElement<SourceSetInheritanceExtension> {

    /**
     * Makes this source set inherit the dependencies of the provided source set.
     * @param sourceSet The source set to inherit from
     */
    void from(SourceSet sourceSet);
}