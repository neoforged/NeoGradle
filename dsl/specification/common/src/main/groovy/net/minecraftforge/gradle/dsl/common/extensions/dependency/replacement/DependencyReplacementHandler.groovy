package net.minecraftforge.gradle.dsl.common.extensions.dependency.replacement

import groovy.transform.CompileStatic
import net.minecraftforge.gradle.dsl.annotations.DSLProperty
import net.minecraftforge.gradle.dsl.base.BaseDSLElement
import org.gradle.api.provider.Property
import org.jetbrains.annotations.NotNull

/**
 * Defines a handler for dependency replacements.
 */
@CompileStatic
interface DependencyReplacementHandler extends BaseDSLElement<DependencyReplacementHandler> {

    /**
     * The name of the dependency replacement handler.
     *
     * @return The name.
     */
    @NotNull
    String getName();

    /**
     * The handlers dependency replacer.
     *
     * @return The replacer.
     */
    @DSLProperty
    Property<DependencyReplacer> getReplacer();
}
