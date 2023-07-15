package net.neoforged.gradle.dsl.common.runs.run

import groovy.transform.CompileStatic
import net.minecraftforge.gdi.BaseDSLElement
import net.minecraftforge.gdi.annotations.ClosureEquivalent
import net.minecraftforge.gdi.annotations.DSLProperty
import org.gradle.api.Action
import org.gradle.api.artifacts.Dependency
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input

/**
 * A custom dependency handler which manages runtime dependencies for a run configuration.
 */
@CompileStatic
interface DependencyHandler extends BaseDSLElement<DependencyHandler> {

    /**
     * Gets the runtime dependencies for the run configuration.
     * @return The runtime dependencies for the run configuration.
     */
    @Input
    @DSLProperty
    ListProperty<RunDependency> getRuntime();

    /**
     * Adds a runtime dependency to the run configuration.
     *
     * @param dependencyNotation The dependency notation.
     * @return The runtime dependency.
     */
    RunDependency runtime(Object dependencyNotation);

    /**
     * Adds a runtime dependency to the run configuration.
     *
     * @param dependencyNotation The dependency notation.
     * @param configureClosure The closure to configure the runtime dependency.
     * @return The runtime dependency.
     */
    @ClosureEquivalent
    RunDependency runtime(Object dependencyNotation, Action<Dependency> configureClosure);

    /**
     * Creates a new run dependency from the given notation.
     *
     * @param dependencyNotation The run dependency notation.
     * @return The run dependency.
     */
    RunDependency create(Object dependencyNotation);

    /**
     * Creates a new run dependency from the given notation and configures it.
     *
     * @param dependencyNotation The run dependency notation.
     * @param configureClosure The closure to configure the run dependency.
     * @return The run dependency.
     */
    @ClosureEquivalent
    RunDependency create(Object dependencyNotation, Action<Dependency> configureClosure);

    /**
     * Creates a new run dependency from the given module notation.
     *
     * @param notation the module notation.
     * @return The run dependency.
     */
    RunDependency module(Object notation);

    /**
     * Creates a new run dependency from the given module notation and configures it.
     *
     * @param notation the module notation.
     * @param configureClosure The closure to configure the module dependency.
     * @return The run dependency.
     */
    @ClosureEquivalent
    RunDependency module(Object notation, Action<Dependency> configureClosure);

    /**
     * Creates a new run dependency from the given project notation.
     *
     * @param notation the project notation.
     * @return The run dependency.
     */
    RunDependency project(Map<String, ?> notation);
}