package net.neoforged.gradle.dsl.common.extensions.repository

import net.minecraftforge.gdi.BaseDSLElement
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency

/**
 * Defines an entry for a dummy repository.
 */
interface Entry extends BaseDSLElement<Entry> {

    /**
     * @returns the original dependency that this entry represents.
     */
    Dependency getOriginal();

    /**
     * @returns the dependency that this entry represents.
     */
    Dependency getDependency()

    /**
     * @returns the configuration that this entry depends on.
     */
    Configuration getDependencies()

    /**
     * @returns true if this entry has sources.
     */
    boolean hasSources()

    /**
     * A builder for creating entries.
     */
    interface Builder extends BaseDSLElement<Entry> {

        /**
         * Configures the entry that is about to be created from the given dependency.
         *
         * @param dependency The dependency to set.
         * @return The builder.
         */
        Builder from(Dependency dependency)

        /**
         * Configures the entry that is about to be created from the given dependency and compileDependencies.
         *
         * @param dependency The dependency to set.
         * @param dependencies The compileDependencies to set.
         * @return The builder.
         */
        Builder from(Dependency dependency, Configuration dependencies);

        /**
         * Configures the entry that is about to be created to not have any sources.
         * @return The builder.
         */
        Builder withoutSources();

        /**
         * @returns the entry that was built.
         */
        Entry build();
    }
}