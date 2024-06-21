package net.neoforged.gradle.dsl.common.dependency

import groovy.transform.CompileStatic
import org.gradle.api.specs.Spec

@CompileStatic
interface DependencyFilter extends DependencyManagementObject {

    /**
     * Exclude compileDependencies that match the provided spec.
     * If at least one exclude spec is provided, only the compileDependencies which fail the check will be excluded.
     *
     * @param spec The spec to exclude compileDependencies that match.
     * @return The filter (this object).
     */
    DependencyFilter exclude(Spec<? super ArtifactIdentifier> spec);

    /**
     * Include compileDependencies that match the provided spec.
     * If at least one include spec is supplied then only compileDependencies that match the include-spec will be included.
     *
     * @param spec The spec to include compileDependencies that match.
     * @return The filter (this object)
     */
    DependencyFilter include(Spec<? super ArtifactIdentifier> spec);

    /**
     * Indicates if the given identifier passes the filter.
     *
     * @param dependency The resolved dependency to check.
     * @return The result of the filter.
     */
    boolean isIncluded(ArtifactIdentifier dependency);
}
