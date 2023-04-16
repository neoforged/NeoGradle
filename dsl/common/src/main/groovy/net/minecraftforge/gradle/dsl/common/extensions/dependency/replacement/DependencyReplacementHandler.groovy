package net.minecraftforge.gradle.dsl.common.extensions.dependency.replacement

import groovy.transform.CompileStatic
import net.minecraftforge.gdi.BaseDSLElement
import net.minecraftforge.gdi.NamedDSLElement
import net.minecraftforge.gdi.annotations.DSLProperty
import org.gradle.api.provider.Property

/**
 * Defines a handler for dependency replacements.
 */
@CompileStatic
interface DependencyReplacementHandler extends BaseDSLElement<DependencyReplacementHandler>, NamedDSLElement {

    /**
     * The handlers dependency replacer.
     *
     * @return The replacer.
     */
    @DSLProperty
    Property<DependencyReplacer> getReplacer();
}
