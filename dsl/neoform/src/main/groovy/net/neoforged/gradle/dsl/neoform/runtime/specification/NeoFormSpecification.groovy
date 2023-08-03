package net.neoforged.gradle.dsl.neoform.runtime.specification

import groovy.transform.CompileStatic
import net.neoforged.gradle.dsl.common.runtime.spec.Specification
import net.neoforged.gradle.dsl.common.util.Artifact;
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider;
import org.jetbrains.annotations.NotNull;

/**
 * Defines an NeoForm specific runtime specification.
 */
@CompileStatic
interface NeoFormSpecification extends Specification {

    /**
     * Defines the NeoForm artifact that is used to build the specification.
     * 
     * @return The NeoForm artifact.
     */
    @NotNull
    Artifact getNeoFormArtifact();
    
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
         * Configures the specification for use with the given NeoForm Group, extracted from the given provider.
         *
         * @param neoFormGroup The NeoForm Group provider.
         * @return The builder.
         */
        @NotNull
        B withNeoFormGroup(@NotNull final Provider<String> neoFormGroup);

        /**
         * Configures the specification for use with the given NeoForm Group.
         *
         * @param neoFormGroup The NeoForm Group to use.
         * @return The builder.
         */
        @NotNull
        B withNeoFormGroup(@NotNull final String neoFormGroup);

        /**
         * Configures the specification for use with the given NeoForm Name, extracted from the given provider.
         *
         * @param neoFormName The NeoForm Name provider.
         * @return The builder.
         */
        @NotNull
        B withNeoFormName(@NotNull final Provider<String> neoFormName);

        /**
         * Configures the specification for use with the given NeoForm Name.
         *
         * @param neoFormName The NeoForm Name to use.
         * @return The builder.
         */
        @NotNull
        B withNeoFormName(@NotNull final String neoFormName);
        
        /**
         * Configures the specification for use with the given NeoForm version, extracted from the given provider.
         *
         * @param neoFormVersion The NeoForm version provider.
         * @return The builder.
         */
        @NotNull
        B withNeoFormVersion(@NotNull final Provider<String> neoFormVersion);

        /**
         * Configures the specification for use with the given NeoForm version.
         *
         * @param neoFormVersion The NeoForm version to use.
         * @return The builder.
         */
        @NotNull
        B withNeoFormVersion(@NotNull final String neoFormVersion);

        /**
         * Configures the specification for use with the given NeoForm Artifact, extracted from the given provider.
         *
         * @param neoFormArtifact The NeoForm Artifact provider.
         * @return The builder.
         */
        @NotNull
        B withNeoFormArtifact(@NotNull final Provider<Artifact> neoFormArtifact);

        /**
         * Configures the specification for use with the given NeoForm Artifact.
         *
         * @param neoFormArtifact The NeoForm Artifact to use.
         * @return The builder.
         */
        @NotNull
        B withNeoFormArtifact(@NotNull final Artifact neoFormArtifact);

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
