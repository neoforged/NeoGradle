package net.minecraftforge.gradle.dsl.common.extensions.repository

import groovy.transform.CompileStatic
import net.minecraftforge.gdi.BaseDSLElement
import net.minecraftforge.gradle.dsl.common.util.ModuleReference
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.ResolvedDependency
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable

/**
 * Defines a reference to an entry in a repository.
 * Generally used as a list of dependencies in a dummy repository entry.
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
     * Gives access to the name of the dependency.
     *
     * @return The name.
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
    ModuleReference toModuleReference ( ) ;

    /**
     * Defines a builder which can be used to create a repository reference.
     *
     * @param <TSelf> The self-reference type of the builder.
     * @param <TDependency> The type for the dependencies of the entry.
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
         * The currently configured name in the builder.
         *
         * @return The currently configured name in the builder.
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
         * Sets the name of the reference.
         *
         * @param name The new name for the reference.
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
