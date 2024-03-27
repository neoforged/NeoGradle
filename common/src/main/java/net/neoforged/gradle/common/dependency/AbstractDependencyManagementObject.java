package net.neoforged.gradle.common.dependency;

import groovy.lang.Closure;
import net.neoforged.gradle.dsl.common.dependency.DependencyManagementObject;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.dsl.DependencyFactory;
import org.gradle.api.specs.Spec;
import org.gradle.api.specs.Specs;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.util.Objects;

@SuppressWarnings("UnstableApiUsage")
public abstract class AbstractDependencyManagementObject implements DependencyManagementObject {
    @Inject
    protected abstract DependencyFactory getDependencyFactory();

    protected static ArtifactIdentifier createArtifactIdentifier(final Dependency dependency) {
        return new ArtifactIdentifier(dependency.getGroup(), dependency.getName(), dependency.getVersion());
    }

    @Override
    public Spec<? super ArtifactIdentifier> dependency(Project project) {
        return dependency(getDependencyFactory().create(project));
    }

    @Override
    public Spec<? super ArtifactIdentifier> dependency(CharSequence dependencyNotation) {
        return dependency(getDependencyFactory().create(dependencyNotation));
    }

    @Override
    public Spec<? super ArtifactIdentifier> dependency(@Nullable String group, String name, @Nullable String version) {
        return dependency(getDependencyFactory().create(group, name, version));
    }

    @Override
    public Spec<? super ArtifactIdentifier> dependency(Dependency dependency) {
        ArtifactIdentifier identifier = createArtifactIdentifier(dependency);

        return this.dependency(new Closure<Boolean>(null) {

            @SuppressWarnings("ConstantConditions")
            @Override
            public Boolean call(final Object it) {
                if (it instanceof DependencyManagementObject.ArtifactIdentifier) {
                    final DependencyManagementObject.ArtifactIdentifier id = (DependencyManagementObject.ArtifactIdentifier) it;
                    return (identifier.getGroup() == null || id.getGroup() == null || Objects.equals(identifier.getGroup(), id.getGroup())) &&
                            (identifier.getName() == null || id.getName() == null || Objects.equals(identifier.getName(), id.getName())) &&
                            (identifier.getVersion() == null || id.getVersion() == null || Objects.equals(identifier.getVersion(), id.getVersion()));
                }

                return false;
            }
        });
    }

    @Override
    public Spec<? super ArtifactIdentifier> dependency(Closure<Boolean> spec) {
        return Specs.convertClosureToSpec(spec);
    }
}
