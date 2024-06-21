package net.neoforged.gradle.dsl.common.extensions.repository

import groovy.transform.CompileStatic
import net.minecraftforge.gdi.BaseDSLElement
import net.neoforged.gradle.dsl.common.util.ModuleReference
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.ResolvedDependency
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable

/**
 * Defines a reference to an entry in a repository.
 * Generally used as a list of compileDependencies in a dummy repository entry.
 *
 * @param <TSelf> The self-reference type of the entry.
 */
@CompileStatic
interface RepositoryReference {

    /**
     * Gives access to the group id of the dependency.
     *
     * @return The group id.
     */
    @NotNull
    String getGroup()

    /**
     * Gives access to the identifier of the dependency.
     *
     * @return The identifier.
     */
    @NotNull
    String getName()

    /**
     * Gives access to the version of the dependency.
     *
     * @return The version.
     */
    @NotNull
    String getVersion()

    /**
     * Gives access to the classifier of the dependency.
     *
     * @return The classifier.
     */
    @Nullable
    String getClassifier()

    /**
     * Gives access to the extension of the dependency.
     *
     * @return The extension.
     */
    @Nullable
    String getExtension()

    /**
     * The unique comparable reference to this repository reference.
     *
     * @return The reference comparable with other repository systems.
     */
    @NotNull
    ModuleReference toModuleReference();

    /**
     * Converts the current entry into a gradle dependency.
     *
     * @param project The project which the dependency should be created for.
     * @return The gradle dependency.
     */
    @NotNull
    Dependency toGradle(Project project)

    /**
     * Defines a builder which can be used to create a repository reference.
     *
     * @param <TSelf> The self-reference type of the builder.
     * @param <TDependency> The type for the compileDependencies of the entry.
     */
    @CompileStatic
    interface Builder<TSelf extends Builder<TSelf, TDependency>, TDependency extends RepositoryReference> extends BaseDSLElement<TSelf> {

        /**
         * The currently configured group in the builder.
         *
         * @return The currently configured group in the builder.
         */
        @NotNull
        String getGroup();

        /**
         * The currently configured identifier in the builder.
         *
         * @return The currently configured identifier in the builder.
         */
        @NotNull
        String getName();

        /**
         * The currently configured version in the builder.
         *
         * @return The currently configured version in the builder.
         */
        @NotNull
        String getVersion();

        /**
         * The currently configured classifier in the builder.
         *
         * @return The currently configured classifier in the builder.
         */
        @Nullable
        String getClassifier();

        /**
         * The currently configured extension in the builder.
         *
         * @return The currently configured extension in the builder.
         */
        @Nullable
        String getExtension();

        /**
         * Sets the group of the reference.
         *
         * @param group The new group for the reference.
         * @return The builder invoked on.
         */
        @NotNull
        TSelf setGroup(@NotNull String group);

        /**
         * Sets the identifier of the reference.
         *
         * @param name The new identifier for the reference.
         * @return The builder invoked on.
         */
        @NotNull
        TSelf setName(@NotNull String name);

        /**
         * Sets the version of the reference.
         *
         * @param version The new version for the reference.
         * @return The builder invoked on.
         */
        @NotNull
        TSelf setVersion(@NotNull String version);

        /**
         * Sets the classifier of the reference.
         *
         * @param classifier The new classifier for the reference.
         * @return The builder invoked on.
         */
        @NotNull
        TSelf setClassifier(@Nullable String classifier);

        /**
         * Sets the extension of the reference.
         *
         * @param extension The new extension for the reference.
         * @return The builder invoked on.
         */
        @NotNull
        TSelf setExtension(@Nullable String extension);

        /**
         * Copies the values from the given gradle dependency into the builder.
         *
         * @param dependency The dependency to copy the values from.
         * @return The builder invoked on.
         */
        @NotNull
        TSelf from(@NotNull ModuleDependency dependency);

        /**
         * Copies the values from the given resolved dependency into the builder.
         *
         * @param dependency The dependency to copy the values from.
         * @return The builder invoked on.
         */
        @NotNull
        TSelf from(@NotNull ResolvedDependency resolvedDependency);

        /**
         * Creates a copy of the current builder.
         *
         * @return The copy of the current builder.
         */
        @NotNull
        TSelf but();
    }
}
