package net.neoforged.gradle.dsl.common.extensions.dependency.replacement

import groovy.transform.CompileStatic
import net.minecraftforge.gdi.BaseDSLElement
import net.minecraftforge.gdi.annotations.DSLProperty
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.jetbrains.annotations.NotNull

/**
 * Defines an extension which handles the dependency replacements.
 */
@CompileStatic
interface DependencyReplacement extends BaseDSLElement<DependencyReplacement> {

    /**
     * Invoked to handle a given configuration.
     *
     * @param configuration The configuration to handle.
     */
    void handleConfiguration(Configuration configuration);

    /**
     * The dependency replacement handlers.
     *
     * @return The handlers.
     */
    @NotNull
    @DSLProperty
    NamedDomainObjectContainer<DependencyReplacementHandler> getReplacementHandlers();

    /**
     * Optionally converts the given dependency back to the original dependency it replaced.
     *
     * @param dependency The dependency to optionally convert back.
     * @param configuration The configuration the given dependency can be found it resides in.
     * @return The original dependency if it can be converted back, otherwise the given dependency.
     */
    @NotNull
    Dependency optionallyConvertBackToOriginal(Dependency dependency, Configuration configuration)
}
