package net.minecraftforge.gradle.dsl.common.extensions.dependency.replacement

import groovy.transform.CompileStatic
import net.minecraftforge.gdi.BaseDSLElement
import net.minecraftforge.gdi.annotations.DSLProperty
import org.gradle.api.NamedDomainObjectContainer
import org.jetbrains.annotations.NotNull

/**
 * Defines an extension which handles the dependency replacements.
 */
@CompileStatic
interface DependencyReplacement extends BaseDSLElement<DependencyReplacement> {

    /**
     * The dependency replacement handlers.
     *
     * @return The handlers.
     */
    @NotNull
    @DSLProperty
    NamedDomainObjectContainer<DependencyReplacementHandler> getReplacementHandlers();
}
