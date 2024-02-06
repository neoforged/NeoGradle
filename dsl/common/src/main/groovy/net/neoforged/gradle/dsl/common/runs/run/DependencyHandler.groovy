package net.neoforged.gradle.dsl.common.runs.run

import groovy.transform.CompileStatic
import net.minecraftforge.gdi.BaseDSLElement
import net.minecraftforge.gdi.annotations.ClosureEquivalent
import org.gradle.api.Action
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.tasks.Internal

/**
 * A custom dependency handler which manages runtime dependencies for a run configuration.
 */
@CompileStatic
interface DependencyHandler extends BaseDSLElement<DependencyHandler> {
    /**
     * The dependency configuration that contains all the declared dependencies.
     */
    @Internal
    Configuration getConfiguration();

    /**
     * Adds a runtime dependency to the run configuration.
     *
     * @param dependencyNotation The dependency notation.
     * @return The runtime dependency.
     */
    Dependency runtime(Object dependencyNotation);

    /**
     * Adds a runtime dependency to the run configuration.
     *
     * @param dependencyNotation The dependency notation.
     * @param configureClosure The closure to configure the runtime dependency.
     * @return The runtime dependency.
     */
    @ClosureEquivalent
    Dependency runtime(Object dependencyNotation, Action<Dependency> configureClosure);

    /**
     * Creates a new run dependency from the given notation.
     *
     * @param dependencyNotation The run dependency notation.
     * @return The run dependency.
     */
    Dependency create(Object dependencyNotation);

    /**
     * Creates a new run dependency from the given notation and configures it.
     *
     * @param dependencyNotation The run dependency notation.
     * @param configureClosure The closure to configure the run dependency.
     * @return The run dependency.
     */
    @ClosureEquivalent
    Dependency create(Object dependencyNotation, Action<Dependency> configureClosure);

    /**
     * Creates a new run dependency from the given module notation.
     *
     * @param notation the module notation.
     * @return The run dependency.
     */
    Dependency module(Object notation);

    /**
     * Creates a new run dependency from the given module notation and configures it.
     *
     * @param notation the module notation.
     * @param configureClosure The closure to configure the module dependency.
     * @return The run dependency.
     */
    @ClosureEquivalent
    Dependency module(Object notation, Action<Dependency> configureClosure);

    /**
     * Creates a new run dependency from the given project notation.
     *
     * @param notation the project notation.
     * @return The run dependency.
     */
    Dependency project(Map<String, ?> notation);
}