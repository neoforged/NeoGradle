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
     * Gives access to the task that produces the raw jar for the given dependency in our dynamic repository.
     *
     * @param dependency The dependency to get the raw jar producing task for.
     * @param configuration The configuration to get the raw jar producing task for.
     * @return The raw jar dependency that will trigger the copying of the raw jar into the repository.
     */
    @NotNull
    Dependency getRawJarDependency(Dependency dependency, Configuration configuration);

    /**
     * Gives access to the task that produces the sources jar for the given dependency in our dynamic repository.
     *
     * @param dependency The dependency to get the sources jar producing task for.
     * @param configuration The configuration to get the sources jar producing task for.
     * @return The sources jar dependency that will trigger the copying the sources jar into the repository.
     */
    @NotNull
    Dependency getSourcesJarDependency(Dependency dependency, Configuration configuration);
}
