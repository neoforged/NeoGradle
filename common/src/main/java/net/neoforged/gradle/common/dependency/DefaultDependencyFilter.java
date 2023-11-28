package net.neoforged.gradle.common.dependency;

import net.neoforged.gradle.dsl.common.dependency.DependencyFilter;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.artifacts.ResolvedDependency;
import org.gradle.api.specs.Spec;

import java.util.ArrayList;
import java.util.List;

public class DefaultDependencyFilter extends AbstractDependencyManagementObject implements DependencyFilter {
    protected final List<Spec<? super ArtifactIdentifier>> includeSpecs = new ArrayList<>();
    protected final List<Spec<? super ArtifactIdentifier>> excludeSpecs = new ArrayList<>();

    public DefaultDependencyFilter(Project project) {
        super(project);
    }

    @Override
    public DependencyFilter exclude(Spec<? super ArtifactIdentifier> spec) {
        excludeSpecs.add(spec);
        return this;
    }

    @Override
    public DependencyFilter include(Spec<? super ArtifactIdentifier> spec) {
        includeSpecs.add(spec);
        return this;
    }

    @Override
    public boolean isIncluded(ResolvedDependency dependency) {
        return isIncluded(createArtifactIdentifier(dependency));
    }

    @Override
    public boolean isIncluded(ModuleDependency dependency) {
        return isIncluded(createArtifactIdentifier(dependency));
    }

    @Override
    public boolean isIncluded(ArtifactIdentifier dependency) {
        boolean include = includeSpecs.isEmpty() || includeSpecs.stream().anyMatch(spec -> spec.isSatisfiedBy(dependency));
        boolean exclude = !excludeSpecs.isEmpty() && excludeSpecs.stream().anyMatch(spec -> spec.isSatisfiedBy(dependency));
        return include && !exclude;
    }
}
