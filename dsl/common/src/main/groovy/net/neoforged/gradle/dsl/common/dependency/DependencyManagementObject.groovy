package net.neoforged.gradle.dsl.common.dependency

import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.FileCollectionDependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.file.FileCollection
import org.gradle.api.specs.Spec

import javax.annotation.Nullable

@CompileStatic
interface DependencyManagementObject {
    /**
     * Create a spec that matches compileDependencies using the provided notation on group, identifier, and version
     *
     * @param notation The dependency notation to parse.
     * @return The spec that matches the dependency notation.
     */
    Spec<? super ArtifactIdentifier> dependency(CharSequence dependencyNotation);

    /**
     * Create a spec that matches compileDependencies on the provided group, identifier, and version
     *
     * @param notation The dependency notation to parse.
     * @return The spec that matches the dependency notation.
     */
    Spec<? super ArtifactIdentifier> dependency(@Nullable String group, String name, @Nullable String version);

    /**
     * Create a spec that matches compileDependencies using the provided project's group, identifier, and version
     *
     * @param notation The dependency notation to parse.
     * @return The spec that matches the dependency notation.
     */
    Spec<? super ArtifactIdentifier> dependency(Project project);

    /**
     * Create a spec that matches the provided dependency on group, identifier, and version
     *
     * @param dependency The dependency to match.
     * @return The spec that matches the dependency.
     */
    Spec<? super ArtifactIdentifier> dependency(Dependency dependency);

    /**
     * Create a spec that matches the provided closure.
     *
     * @param spec The closure to invoke.
     * @return The spec that matches by invoking the closure.
     */
    Spec<? super ArtifactIdentifier> dependency(Closure<Boolean> spec);

    /**
     * Simple artifact identifier class which only references group, identifier and version.
     */
    @CompileStatic
    @EqualsAndHashCode(includeFields = true)
    final class ArtifactIdentifier {
        private final String group
        private final String name
        private final String version

        /**
         * Creates a new instance of the given artifact details.
         *
         * @param group   The group of the artifact to identify.
         * @param name    The identifier of the artifact to identify.
         * @param version The version of the artifact to identify.
         */
        ArtifactIdentifier(String group, String name, String version) {
            this.group = group
            this.name = name
            this.version = version
        }

        /**
         * Gets the group of the artifact.
         *
         * @return The group of the artifact.
         */
        String getGroup() {
            return group
        }

        /**
         * Gets the identifier of the artifact.
         *
         * @return The identifier of the artifact.
         */
        String getName() {
            return name
        }

        /**
         * Gets the version of the artifact.
         *
         * @return The version of the artifact.
         */
        String getVersion() {
            return version
        }
    }
}
