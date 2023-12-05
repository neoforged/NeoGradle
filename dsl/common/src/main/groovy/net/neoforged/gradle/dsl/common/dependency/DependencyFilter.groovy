package net.neoforged.gradle.dsl.common.dependency

import groovy.transform.CompileStatic
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.specs.Spec

@CompileStatic
interface DependencyFilter extends DependencyManagementObject {

    /**
     * Exclude dependencies that match the provided spec.
     * If at least one exclude spec is provided, only the dependencies which fail the check will be excluded.
     *
     * @param spec The spec to exclude dependencies that match.
     * @return The filter (this object).
     */
    DependencyFilter exclude(Spec<? super ArtifactIdentifier> spec);

    /**
     * Include dependencies that match the provided spec.
     * If at least one include spec is supplied then only dependencies that match the include-spec will be included.
     *
     * @param spec The spec to include dependencies that match.
     * @return The filter (this object)
     */
    DependencyFilter include(Spec<? super ArtifactIdentifier> spec);

    /**
     * Indicates if the given resolved dependency passes the filter.
     *
     * @param dependency The resolved dependency to check.
     * @return The result of the filter.
     */
    boolean isIncluded(ResolvedDependency dependency);

    /**
     * Indicates if the given dependency passes the filter.
     *
     * @param dependency The dependency to check.
     * @return The result of the filter.
     */
    boolean isIncluded(ModuleDependency dependency);

    /**
     * Checks if the given artifact identifier matches the dependency.
     *
     * @param identifier The identifier to check.
     * @return The result of the filter.
     */
    boolean isIncluded(ArtifactIdentifier identifier);
}
