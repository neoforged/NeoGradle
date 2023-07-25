package net.neoforged.gradle.dsl.common.runtime.naming

import groovy.transform.CompileStatic

/**
 * Factory interface which can construct a string based dependency notation for
 * a given version payload and classifier.
 */
@FunctionalInterface
@CompileStatic
interface DependencyNotationVersionManager {

    /**
     * Encodes a the current version setup into a single string compatible with a maven notation.
     *
     * @param versionPayload The version payload that should be turned into a dependency notation version.
     * @return The encoded version
     */
    String encode(final Map<String, String> versionPayload)

    /**
     * Parses the version information stored in a encoded version.
     *
     * @param version The encoded version information.
     * @return The decoded version map.
     */
    Map<String, String> decode(final String version);
}