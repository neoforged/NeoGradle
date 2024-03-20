package net.neoforged.gradle.dsl.common.extensions

import net.neoforged.gradle.dsl.common.dependency.DependencyFilter
import net.neoforged.gradle.dsl.common.dependency.DependencyVersionInformationHandler
import org.gradle.api.Action
import org.gradle.api.artifacts.Dependency
import org.gradle.api.publish.maven.MavenPublication

interface JarJar {

    String EXTENSION_NAME = "jarJar";

    /**
     * Enable the jarJar default configuration, unless already disabled
     */
    void enable();

    /**
     * Disable the jarJar default configuration
     */
    void disable();

    /**
     *
     * @param disable or un-disable the jarJar default configuration; allows reversing {@link #disable()}.
     */
    void disable(boolean disable);

    /**
     * {@return whether the jarJar task should by default copy the contents and manifest of the jar task}
     */
    boolean getDefaultSourcesDisabled();

    /**
     * Stop the jarJar task from copying the contents and manifest of the jar task
     */
    void disableDefaultSources();

    /**
     * Set whether the jarJar task should copy the contents and manifest of the jar task
     * @param value whether to disable the default sources
     */
    void disableDefaultSources(boolean value);

    /**
     * Set the jarJar task to source dependencies to include from the runtime classpath
     */
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

    /**
     * Filter the dependencies included by the jarJar task
     */
    JarJar dependencies(Action<DependencyFilter> c);

    /**
     * Configure the versions used by the dependencies included by the jarJar task
     */
    JarJar versionInformation(Action<DependencyVersionInformationHandler> c);

}
