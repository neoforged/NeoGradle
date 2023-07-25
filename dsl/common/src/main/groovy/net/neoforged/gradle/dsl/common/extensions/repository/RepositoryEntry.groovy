package net.neoforged.gradle.dsl.common.extensions.repository

import com.google.common.collect.ImmutableSet
import groovy.transform.CompileStatic
import net.minecraftforge.gdi.BaseDSLElement
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable

import java.nio.file.Path
import java.util.function.Consumer

/**
 * Defines an entry for a dummy repository.
 *
 * @param <TSelf> The self-reference type of the entry.
 * @param <TDependency> The type for the dependencies of the entry.
 */
@CompileStatic
interface RepositoryEntry<TSelf extends RepositoryEntry<TSelf, TDependency>, TDependency extends RepositoryReference> extends Serializable, RepositoryReference {

    /**
     * Indicates if the entry matches a given dependency in gradles dependency management system.
     *
     * @param id The id of the dependency to check against.
     * @return true, when the entry matches the dependency, false otherwise.
     */
    boolean matches(ModuleComponentIdentifier id)

    /**
     * Creates a new unregistered entry which is a copy of the current entry, however which represents the sources
     * classified dependency of the current entry.
     *
     * @return The new unregistered entry.
     */
    @NotNull
    TSelf asSources()

    /**
     * Builds a path which references the file that is represented by the current entry.
     *
     * @param baseDir The base directory of the dummy repository this entry is supposed to be in.
     * @return The path to the file.
     * @throws IOException When the path could not be constructed.
     */
    @NotNull
    Path buildArtifactPath(Path baseDir) throws IOException

    /**
     * Builds a file path which defines the path of the file relative to repository root that is represented by the current entry.
     *
     * @return The path to the file.
     * @throws IOException When the path could not be constructed.
     */
    @NotNull
    String buildArtifactPath()

    /**
     * Defines the full group id of the entry.
     * Generally does not match the group id of the dependency that this entry represents.
     *
     * @return The full group id of the entry.
     */
    @NotNull
    String getFullGroup()

    /**
     * Defines the normal group id of the entry.
     * Generally does match the group id of the dependency that this entry represents.
     *
     * @return The normal group id of the entry.
     */
    @NotNull
    String getGroup()

    /**
     * Defines the name of the entry.
     *
     * @return The name of the entry.
     */
    @NotNull
    String getName()

    /**
     * Defines the version of the entry.
     *
     * @return The version of the entry.
     */
    @NotNull
    String getVersion()

    /**
     * Defines the classifier of the entry.
     * Might be null.
     *
     * @return The classifier of the entry.
     */
    @Nullable
    String getClassifier()

    /**
     * Defines the extension of the entry.
     * Might be null.
     *
     * @return The extension of the entry.
     */
    @Nullable
    String getExtension()

    /**
     * Defines the dependencies of the entry.
     *
     * @return The dependencies of the entry.
     */
    @NotNull
    Collection<? extends RepositoryReference> getDependencies()

    /**
     * Defines a builder for the repository entry.
     *
     * @param <TSelf> The self-reference type of the builder.
     * @param <TDependency> The type for the dependencies of the entry.
     * @param <TDependencyBuilder> The type for the dependency builders.
     */
    @CompileStatic
    interface Builder<TSelf extends Builder<TSelf, TDependency, TDependencyBuilder>, TDependency extends RepositoryReference, TDependencyBuilder extends RepositoryReference.Builder<TDependencyBuilder, TDependency>> extends RepositoryReference.Builder<TSelf, TDependency>, BaseDSLElement<TSelf>, Serializable {

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
         * The currently configured dependencies in the builder.
         *
         * @return The currently configured dependencies in the builder.
         */
        @NotNull
        ImmutableSet<? extends RepositoryReference> getDependencies();

        /**
         * Configures the group of the repository entry.
         *
         * @param group The group.
         * @return The builder invoked on.
         */
        @NotNull
        TSelf setGroup(@NotNull String group);

        /**
         * Sets the name of the entry.
         *
         * @param name The new name for the entry.
         * @return The builder invoked on.
         */
        @NotNull
        TSelf setName(@NotNull String name);

        /**
         * Sets the version of the entry.
         *
         * @param version The new version for the entry.
         * @return The builder invoked on.
         */
        @NotNull
        TSelf setVersion(@NotNull String version);

        /**
         * Sets the classifier of the entry.
         *
         * @param classifier The new classifier for the entry.
         * @return The builder invoked on.
         */
        @NotNull
        TSelf setClassifier(@Nullable String classifier);

        /**
         * Sets the extension of the entry.
         *
         * @param extension The new extension for the entry.
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
         * Adds a dependencies to the entry.
         *
         * @param dependency The dependencies to add.
         * @return The builder invoked on.
         */
        @NotNull
        TSelf setDependencies(@NotNull Collection<? extends RepositoryReference> dependencies);

        /**
         * Adds a dependencies to the entry.
         *
         * @param dependency The dependencies to add.
         * @return The builder invoked on.
         */
        @NotNull
        TSelf setDependencies(@NotNull RepositoryReference... dependencies);

        /**
         * Adds a dependencies to the entry.
         *
         * @param dependency The dependencies to add.
         * @return The builder invoked on.
         */
        @NotNull
        TSelf withDependency(@NotNull Consumer<TDependencyBuilder> consumer);

        /**
         * Adds a dependencies to the entry, in such a way that it is considered a processed dependency and that dependency replacement logic needs to be considered.
         *
         * @param dependency The dependencies to add.
         * @return The builder invoked on.
         */
        @NotNull
        TSelf withProcessedDependency(@NotNull Consumer<TSelf> consumer);

        /**
         * Creates a copy of the current builder.
         *
         * @return The copy of the current builder.
         */
        @NotNull
        TSelf but();
    }
}
