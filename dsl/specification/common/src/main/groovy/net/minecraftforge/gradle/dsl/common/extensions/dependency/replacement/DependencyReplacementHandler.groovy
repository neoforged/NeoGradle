package net.minecraftforge.gradle.dsl.common.extensions.dependency.replacement

import groovy.transform.CompileStatic
import net.minecraftforge.gdi.annotations.DSLProperty
import net.minecraftforge.gradle.dsl.base.BaseDSLElement
import net.minecraftforge.gradle.dsl.base.util.NamedDSLElement
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
