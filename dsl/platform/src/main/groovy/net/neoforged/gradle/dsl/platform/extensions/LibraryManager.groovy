package net.neoforged.gradle.dsl.platform.extensions

import net.neoforged.gradle.dsl.platform.model.Library
import org.gradle.api.provider.Provider

/**
 * Defines a manager for libraries.
 */
interface LibraryManager {

    /**
     * Gets the classpath of a library.
     *
     * @param library The library to get the classpath of.
     * @return The classpath of the library.
     */
    Provider<Set<String>> getClasspathOf(Provider<String> library);

    /**
     * Gets the libraries that a library depends on.
     *
     * @param library The library to get the dependencies of.
     * @return The dependencies of the library.
     */
    Provider<Set<Library>> getLibrariesOf(Provider<String> library);

    /**
     * Gets the classpath of a library.
     *
     * @param library The library to get the classpath of.
     * @return The classpath of the library.
     */
    Provider<Set<String>> getClasspathOf(String library);

    /**
     * Gets the libraries that a library depends on.
     *
     * @param library The library to get the dependencies of.
     * @return The dependencies of the library.
     */
    Provider<Set<Library>> getLibrariesOf(String library);
}