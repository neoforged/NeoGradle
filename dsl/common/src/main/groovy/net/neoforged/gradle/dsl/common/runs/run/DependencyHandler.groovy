package net.neoforged.gradle.dsl.common.runs.run

import groovy.transform.CompileStatic
import net.minecraftforge.gdi.BaseDSLElement
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.dsl.Dependencies
import org.gradle.api.artifacts.dsl.DependencyCollector
import org.gradle.api.tasks.Internal

/**
 * A custom dependency handler which manages runtime dependencies for a run configuration.
 */
@CompileStatic
interface DependencyHandler extends BaseDSLElement<DependencyHandler>, Dependencies {
    /**
     * The dependency configuration that contains all the declared runtime dependencies.
     */
    @Internal
    Configuration getRuntimeConfiguration();

    /**
     * The dependency configuration that contains all the declared mod dependencies.
     */
    @Internal
    Configuration getModsConfiguration();

    /**
     * Adds a dependency to the runtime configuration.
     *
     * @return The dependency collector.
     */
    @Internal
    DependencyCollector getRuntime();

    /**
     * Adds a dependency to the mods configuration.
     *
     * @return The dependency collector.
     */
    @Internal
    DependencyCollector getMod();
}