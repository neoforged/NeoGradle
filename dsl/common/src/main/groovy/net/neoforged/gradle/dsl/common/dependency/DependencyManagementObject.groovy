package net.neoforged.gradle.dsl.common.dependency

import groovy.transform.CompileStatic
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.specs.Spec

@CompileStatic
interface DependencyManagementObject {
    /**
     * Create a spec that matches dependencies using the provided notation on group, name, and version
     * If a regex is supplied for any of the group, name, or version, the spec will match if the dependency matches the regex.
     *
     * @param notation The dependency notation to parse.
     * @return The spec that matches the dependency notation.
     */
    Spec<? super ModuleComponentIdentifier> dependency(Object notation);

    /**
     * Create a spec that matches the provided dependency on group, name, and version
     *
     * @param dependency The dependency to match.
     * @return The spec that matches the dependency.
     */
    Spec<? super ModuleComponentIdentifier> dependency(Dependency dependency);

    /**
     * Create a spec that matches the provided closure.
     *
     * @param spec The closure to invoke.
     * @return The spec that matches by invoking the closure.
     */
    Spec<? super ModuleComponentIdentifier> dependency(Closure<Boolean> spec);
}
