package net.minecraftforge.gradle.dsl.common.runtime.extensions

import net.minecraftforge.gradle.dsl.annotations.DSLProperty
import net.minecraftforge.gradle.dsl.annotations.ProjectGetter
import net.minecraftforge.gradle.dsl.common.runtime.definition.Definition
import net.minecraftforge.gradle.dsl.common.runtime.spec.Specification
import net.minecraftforge.gradle.dsl.common.util.DistributionType
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.jetbrains.annotations.NotNull

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
interface CommonRuntimes<S extends Specification, B extends Specification.Builder<S, B>, D extends Definition<S>> {

    /**
     * The project which holds the extension.
     *
     * @return The project.
     */
    @ProjectGetter
    Project getProject();

    /**
     * The default distribution type to use for the runtime specifications.
     *
     * @return The default distribution type.
     */
    @DSLProperty
    Property<DistributionType> getDistributionType();

    /**
     * Gives access to a provider which provides the runtime definitions which are defined in this extension.
     * This provider is live and will be updated when the runtime definitions are added or removed.
     * However the definitions might not be baked yet.
     *
     * @return The provider.
     */
    Provider<Map<String, D>> getRuntimes();

    /**
     * Potentially creates a new runtime based on the specification created by the given builder.
     * If a runtime with the same name already exists, the existing runtime is returned.
     * If the specification of the existing runtime, and the one configured via the builder do not line up, an exception is thrown.
     *
     * @param configurator The configurator which consumes a builder that will create the specification which defines the runtime.
     * @return The runtime definition, unbaked.
     */
    D maybeCreate(Action<B> configurator);

    D maybeCreate(S spec);

    D create(Action<B> configurator);

    D create(S spec);

    D getByName(String name);

    D findByName(String name);

    @NotNull Set<D> findIn(Configuration configuration);
}
