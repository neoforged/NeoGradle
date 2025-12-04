package net.neoforged.gradle.dsl.neoform.runtime.specification

import groovy.transform.CompileStatic
import net.neoforged.gradle.dsl.common.runtime.spec.Specification
import net.neoforged.gradle.dsl.neoform.configuration.NeoFormConfigConfigurationSpecV1
import org.gradle.api.file.FileCollection
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable

import java.util.function.BiConsumer
import java.util.function.Consumer;

/**
 * Defines an NeoForm specific runtime specification.
 */
@CompileStatic
interface NeoFormSpecification extends Specification {

    /**
     * The version of NeoForm that shall be used. Does not include the minecraft version as a prefix, unlike
     * {@link Specification#getVersion()}.
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
     * Returns the task name of the task to use as an output as a replacement of the task with the given name.
     *
     * @param taskName The task name to check for.
     * @return The task name of the task to replace it with, null if not replaced.
     */
    @Nullable
    String skipTaskWith(final String taskName); BiConsumer<List<NeoFormConfigConfigurationSpecV1.Step>, Map<String, NeoFormConfigConfigurationSpecV1.Function>> getMutator ( ) ;

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

        /**
         * Configures this runtime to skip the given task.
         * <p>
         *     The individual behaviour of the skipping is up to the runtime implementation.
         * </p>
         *
         * @param task The task name to skip.
         * @param outputSource The task to take the output of as the output of the source.
         * @return The builder.
         */
        @NotNull
        B withSkippedTask(@NotNull final String task, @NotNull final String outputSource);

        /**
         * Registers a new mutator that can alter the step list and known functions.
         *
         * @param mutator The mutator
         * @return The builder
         */
        B withStepsMutator(@NotNull final BiConsumer<List<NeoFormConfigConfigurationSpecV1.Step>, Map<String, NeoFormConfigConfigurationSpecV1.Function>> mutator)
    }
}
