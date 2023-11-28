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

    void pin(Dependency dependency, String version);

    Optional<String> getPin(Dependency dependency);

    void ranged(Dependency dependency, String range);

    Optional<String> getRange(Dependency dependency);

    JarJar dependencies(Action<DependencyFilter> c);

    JarJar versionInformation(Action<DependencyVersionInformationHandler> c);

    MavenPublication component(MavenPublication mavenPublication);

}
