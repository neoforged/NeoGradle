package net.neoforged.gradle.dsl.neoform.runtime.specification

import groovy.transform.CompileStatic
import net.neoforged.gradle.dsl.common.runtime.spec.Specification
import org.gradle.api.file.FileCollection
import org.jetbrains.annotations.NotNull;

/**
 * Defines an NeoForm specific runtime specification.
 */
@CompileStatic
interface NeoFormSpecification extends Specification {

    /**
     * The version of NeoForm that shall be used.
     * 
     * @return The NeoForm version.
     */
    @NotNull
    String getNeoFormVersion();

    /**
     * A collection of files which need to be added to the recompile classpath,
     * for the recompile phase to succeed.
     *
     * @return The file collection with the additional jars which need to be added
     */
    @NotNull
    FileCollection getAdditionalRecompileDependencies();

    /**
     * Defines a builder for an {@link NeoFormSpecification}.
     *
     * @param <S> The specification type, must be at least an {@link NeoFormSpecification}.
     * @param <B> The self-type of the builder.
     */
    interface Builder<S extends NeoFormSpecification, B extends Builder<S, B>> extends Specification.Builder<S, B> {

        /**
         * Configures the specification to use NeoForm in the given version.
         */
        @NotNull
        default B withNeoFormVersion(@NotNull final String version) {
            withNeoFormDependency(project.getDependencies().create("net.neoforged:neoform:" + version + "@zip"));
        }

        /**
         * Configures the specification for use with the given NeoForm dependency.
         *
         * @param dependencyNotation The coordinates of the NeoForm archive in Gradle notation.
         * @return The builder.
         */
        @NotNull
        B withNeoFormDependency(@NotNull final Object dependencyNotation);

        /**
         * Configures the specification to use the given file collection as additional recompile dependencies.
         *
         * @param files The file collection with the additional recompile dependencies.
         * @return The builder.
         */
        @NotNull
        B withAdditionalDependencies(@NotNull final FileCollection files);
    }
}
