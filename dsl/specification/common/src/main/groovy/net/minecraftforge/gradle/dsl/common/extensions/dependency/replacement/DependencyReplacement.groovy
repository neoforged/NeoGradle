package net.minecraftforge.gradle.dsl.common.extensions.dependency.replacement;

import groovy.transform.CompileStatic;
import net.minecraftforge.gradle.dsl.base.BaseDSLElement
import net.minecraftforge.gradle.dsl.base.util.NamedDSLObjectContainer;
import org.gradle.api.NamedDomainObjectContainer;
import org.jetbrains.annotations.NotNull;

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
    NamedDSLObjectContainer<?, DependencyReplacementHandler> getReplacementHandlers();
}
