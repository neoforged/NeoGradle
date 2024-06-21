package net.neoforged.gradle.dsl.common.runs.run

import groovy.transform.CompileStatic
import net.minecraftforge.gdi.BaseDSLElement
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.dsl.Dependencies
import org.gradle.api.artifacts.dsl.DependencyCollector
import org.gradle.api.tasks.Internal

/**
 * A custom dependency handler which manages runtime compileDependencies for a run configuration.
 */
@CompileStatic
interface DependencyHandler extends BaseDSLElement<DependencyHandler>, Dependencies {
    /**
     * The dependency configuration that contains all the declared runtime compileDependencies.
     */
    @Internal
    Configuration getRuntimeConfiguration();

    /**
     * Adds a dependency to the runtime configuration.
     *
     * @return The dependency collector.
     */
    @Internal
    DependencyCollector getRuntime();
}