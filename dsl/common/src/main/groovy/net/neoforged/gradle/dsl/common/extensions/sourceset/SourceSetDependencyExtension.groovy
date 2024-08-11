package net.neoforged.gradle.dsl.common.extensions.sourceset

import groovy.transform.CompileStatic
import net.minecraftforge.gdi.BaseDSLElement
import org.gradle.api.tasks.SourceSet

/**
 * Represents an extension on a source set that allows for dependency of source sets.
 */
@CompileStatic
interface SourceSetDependencyExtension extends BaseDSLElement<SourceSetDependencyExtension> {

    /**
     * Makes this source set depend on the given sourceset and its dependencies.
     *
     * @param sourceSet The source set to depend on
     */
    void on(SourceSet sourceSet);
}