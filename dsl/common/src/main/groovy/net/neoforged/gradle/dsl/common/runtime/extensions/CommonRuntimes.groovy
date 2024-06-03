package net.neoforged.gradle.dsl.common.runtime.extensions

import groovy.transform.CompileStatic
import net.minecraftforge.gdi.annotations.ClosureEquivalent
import net.minecraftforge.gdi.annotations.ProjectGetter
import net.neoforged.gradle.dsl.common.runtime.definition.LegacyDefinition
import net.neoforged.gradle.dsl.common.runtime.spec.LegacySpecification
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable

/**
 * Defines the extension which handles the runtime specifications and definitions.
 * Runtime definitions are build in a afterEvaluate block.
 *
 * Note there might be many extensions of this subtype in the project.
 * This is because this is the base type of the extension which manages the runtimes
 * for each system type (MCP, Vanilla, Forge etc), see their own specific DSLs for more information
 * and more accurate exposed types.
 *
 * @param <S> The type of the runtime specification.
 * @param <B> The type of the runtime specification builder.
 * @param <D> The type of the runtime definition.
 */
@CompileStatic
interface CommonRuntimes<S extends LegacySpecification, B extends LegacySpecification.Builder<S, B>, D extends LegacyDefinition<S>> {

    /**
     * The project which holds the extension.
     *
     * @return The project.
     */
    @ProjectGetter
    Project getProject();

    /**
     * Gives access to a provider which provides the runtime definitions which are defined in this extension.
     * This provider is live and will be updated when the runtime definitions are added or removed.
     * However the definitions might not be baked yet.
     *
     * @return The provider.
     */
    Map<String, D> getDefinitions();

    /**
     * Potentially creates a new runtime based on the specification created by the given builder.
     * If a runtime with the same name already exists, the existing runtime is returned.
     * If the specification of the existing runtime, and the one configured via the builder do not line up, an exception is thrown.
     *
     * @param configurator The configurator which consumes a builder that will create the specification which defines the runtime.
     * @return The runtime definition, unbaked.
     */
    @ClosureEquivalent
    D maybeCreate(Action<B> configurator);

    /**
     * Potentially creates a new runtime based on the specification created by the given builder.
     * If a runtime with the same name already exists, the existing runtime is returned.
     * If the specification of the existing runtime, and the one configured via the builder do not line up, an exception is thrown.
     *
     * @param dependency The dependency to create the runtime for.
     * @param configurator The configurator which consumes a builder that will create the specification which defines the runtime.
     * @return The runtime definition, unbaked.
     */
    @ClosureEquivalent
    D maybeCreateFor(Dependency dependency, Action < B > configurator);

    /**
     * Creates a new runtime based on the specification created by the given builder.
     * If a runtime with the same name already exists, an exception is thrown.
     *
     * @param configurator The configurator which consumes a builder that will create the specification which defines the runtime.
     * @return The runtime definition, unbaked.
     */
    @ClosureEquivalent
    D create(Action<B> configurator);

    /**
     * Creates a new runtime based on the specification created by the given builder.
     * If a runtime with the same name already exists, an exception is thrown.
     *
     * @param dependency The dependency to create the runtime for.
     * @param configurator The configurator which consumes a builder that will create the specification which defines the runtime.
     * @return The runtime definition, unbaked.
     */
    @ClosureEquivalent
    D create(Dependency dependency, Action<B> configurator);

    /**
     * Looks up a runtime definition by name.
     * Throws an exception of not found.
     *
     * @param name The name of the runtime definition.
     * @return The runtime definition.
     */
    D getByName(String name);

    /**
     * Looks up a runtime definition by name.
     *
     * @param name The name of the runtime definition.
     * @return The runtime definition, or null if not found.
     */
    @Nullable
    D findByNameOrIdentifier(String name);

    /**
     * Tries to find all runtimes which can be found in the given configuration or its dependents.
     *
     * @param configuration The gradle configuration to find the runtimes in.
     * @return The set of runtimes in the configuration.
     */
    @NotNull Set<D> findIn(Configuration configuration);
}
