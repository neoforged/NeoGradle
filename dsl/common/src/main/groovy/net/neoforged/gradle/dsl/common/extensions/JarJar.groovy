package net.neoforged.gradle.dsl.common.extensions

import org.gradle.api.Action
import org.gradle.api.artifacts.Dependency
import org.gradle.api.plugins.JavaPluginExtension

interface JarJar extends JarJarFeature {

    String EXTENSION_NAME = "jarJar";

    /**
     * Sets the version of a dependency contained within a jarJar jar.
     *
     * @param dependency the dependency to mark
     * @param version the version to use within the jar.
     *
     * @deprecated Use <a href="https://docs.gradle.org/current/userguide/rich_versions.html">gradle rich versions</a> instead
     */
    @Deprecated
    void pin(Dependency dependency, String version);

    /**
     * Sets the version range of a dependency that jarJar should accept at runtime.
     *
     * @param dependency the dependency to mark
     * @param range the version range to accept, in the form of a maven version range
     *
     * @deprecated Use <a href="https://docs.gradle.org/current/userguide/rich_versions.html">gradle rich versions</a> instead
     */
    @Deprecated
    void ranged(Dependency dependency, String range);

    /**
     * Configure jarJar for a library feature with the given name, as created by {@link JavaPluginExtension#registerFeature(String,Action)}.
     * Creates a featureJarJar task and configuration for the feature if they are missing.
     * @param featureName the name of the feature to configure
     * @return the configuration for jarJar for the feature
     */
    JarJarFeature forFeature(String featureName);
}
