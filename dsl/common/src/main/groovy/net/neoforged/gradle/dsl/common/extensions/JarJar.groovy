package net.neoforged.gradle.dsl.common.extensions

import net.neoforged.gradle.dsl.common.dependency.DependencyFilter
import net.neoforged.gradle.dsl.common.dependency.DependencyVersionInformationHandler
import org.gradle.api.Action
import org.gradle.api.artifacts.Dependency
import org.gradle.api.publish.maven.MavenPublication

interface JarJar {

    String EXTENSION_NAME = "jarJar";

    void enable();

    void disable();

    void disable(boolean disable);

    boolean getDefaultSourcesDisabled();

    void disableDefaultSources();

    void disableDefaultSources(boolean value);

    void fromRuntimeConfiguration();

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

    JarJar dependencies(Action<DependencyFilter> c);

    JarJar versionInformation(Action<DependencyVersionInformationHandler> c);

}
