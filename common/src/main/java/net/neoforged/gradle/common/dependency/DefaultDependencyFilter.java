package net.neoforged.gradle.common.dependency;

import net.neoforged.gradle.dsl.common.dependency.DependencyFilter;
import org.gradle.api.specs.Spec;

import java.util.ArrayList;
import java.util.List;

public abstract class DefaultDependencyFilter extends AbstractDependencyManagementObject implements DependencyFilter {
    protected final List<Spec<? super ArtifactIdentifier>> includeSpecs = new ArrayList<>();
    protected final List<Spec<? super ArtifactIdentifier>> excludeSpecs = new ArrayList<>();

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
    public boolean isIncluded(ArtifactIdentifier dependency) {
        boolean include = includeSpecs.isEmpty() || includeSpecs.stream().anyMatch(spec -> spec.isSatisfiedBy(dependency));
        boolean exclude = !excludeSpecs.isEmpty() && excludeSpecs.stream().anyMatch(spec -> spec.isSatisfiedBy(dependency));
        return include && !exclude;
    }
}
