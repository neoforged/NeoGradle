package net.minecraftforge.gradle.dsl.mcp.runtime.spec;

import net.minecraftforge.gradle.dsl.common.runtime.spec.Specification;
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider;
import org.jetbrains.annotations.NotNull;

/**
 * Defines an MCP specific runtime specification.
 */
interface McpSpecification extends Specification {

    /**
     * The MCP version that this specification is for.
     *
     * @return The mcp version.
     */
    @NotNull
    String getMcpVersion();

    /**
     * A collection of files which need to be added to the recompile classpath,
     * for the recompile phase to succeed.
     *
     * @return The file collection with the additional jars which need to be added
     */
    @NotNull
    FileCollection getAdditionalRecompileDependencies();

    /**
     * Defines a builder for an {@link McpSpecification}.
     *
     * @param <S> The specification type, must be at least an {@link McpSpecification}.
     * @param <B> The self-type of the builder.
     */
    interface Builder<S extends McpSpecification, B extends Builder<S, B>> extends Specification.Builder<S, B> {

        /**
         * Configures the specification for use with the given MCP version, extracted from the given provider.
         *
         * @param mcpVersion The mcp version provider.
         * @return The builder.
         */
        @NotNull
        Builder withMcpVersion(@NotNull final Provider<String> mcpVersion);

        /**
         * Configures the specification for use with the given MCP version.
         *
         * @param mcpVersion The mcp version to use.
         * @return The builder.
         */
        @NotNull
        Builder withMcpVersion(@NotNull final String mcpVersion);

        /**
         * Configures the specification to use the given file collection as additional recompile dependencies.
         *
         * @param files The file collection with the additional recompile dependencies.
         * @return The builder.
         */
        @NotNull
        Builder withAdditionalDependencies(@NotNull final FileCollection files);
    }
}
