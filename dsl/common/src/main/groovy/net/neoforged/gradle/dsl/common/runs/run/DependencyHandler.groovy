package net.neoforged.gradle.dsl.common.runs.run

import groovy.transform.CompileStatic
import net.minecraftforge.gdi.BaseDSLElement
import net.minecraftforge.gdi.annotations.ClosureEquivalent
import org.gradle.api.Action
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.dsl.Dependencies
import org.gradle.api.artifacts.dsl.DependencyCollector
import org.gradle.api.tasks.Internal

/**
 * A custom dependency handler which manages runtime dependencies for a run configuration.
 */
@CompileStatic
interface DependencyHandler extends BaseDSLElement<DependencyHandler>, Dependencies {
    /**
     * The dependency configuration that contains all the declared dependencies.
     */
    @Internal
    Configuration getConfiguration();

    /**
     * Adds a dependency to the runtime configuration.
     *
     * @return The dependency.
     */
    DependencyCollector getRuntime();
}