package net.neoforged.gradle.dsl.common.runs.run

import groovy.transform.CompileStatic
import net.neoforged.gdi.BaseDSLElement
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.ProjectDependency
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
     * Adds a dependency to the runtime configuration.
     *
     * @return The dependency collector.
     */
    @Internal
    DependencyCollector getRuntime();

    /**
     * Declares a project dependency as a mod source for this run using variant-based resolution.
     * <p>
     * This is the recommended way to add mod sources from other projects when using Gradle's
     * isolated projects mode, as it avoids direct cross-project extension access during configuration time.
     * </p>
     * <p>
     * The {@code configuration} parameter should specify a source set name (e.g., {@code 'main'}).
     * NeoGradle automatically applies the {@code modSource} prefix to resolve the correct variant
     * (e.g., {@code 'modSourceMain'}) if it exists. Standard Java plugin configurations are also supported.
     * </p>
     * <p>
     * Example usage:
     * <pre>{@code
     * runs {
     *     server {
     *         dependencies {
     *             modSource project(path: ':api', configuration: 'main')
     *         }
     *     }
     * }
     * }</pre>
     * </p>
     *
     * @param dependencyNotation The dependency notation, typically a ProjectDependency with a source set name as configuration.
     */
    void modSource(Object dependencyNotation);

    /**
     * Declares a project dependency as a mod source for this run with an explicit group ID.
     * <p>
     * The group ID is used to organize mods in the FML mod list and IDE configurations.
     * </p>
     * <p>
     * Example usage:
     * <pre>{@code
     * runs {
     *     server {
     *         dependencies {
     *             modSource('myModGroup') project(path: ':api', configuration: 'main')
     *         }
     *     }
     * }
     * }</pre>
     * </p>
     *
     * @param groupId The group ID for this mod source.
     * @param dependencyNotation The dependency notation, typically a ProjectDependency with a source set name as configuration.
     */
    void modSource(String groupId, Object dependencyNotation);

    /**
     * The dependency configuration that contains all declared mod source dependencies.
     * <p>
     * This is used internally to resolve variant metadata for cross-project mod sources.
     * </p>
     */
    @Internal
    Configuration getModSourceConfiguration();
}