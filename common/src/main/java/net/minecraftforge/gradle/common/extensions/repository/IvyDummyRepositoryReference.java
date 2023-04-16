package net.minecraftforge.gradle.common.extensions.repository;

import groovy.lang.GroovyObjectSupport;
import net.minecraftforge.gdi.ConfigurableDSLElement;
import net.minecraftforge.gdi.ProjectAssociatedDSLElement;
import net.minecraftforge.gradle.util.ResolvedDependencyUtils;
import net.minecraftforge.gradle.dsl.common.extensions.repository.RepositoryReference;
import org.gradle.api.Project;
import org.gradle.api.artifacts.DependencyArtifact;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.artifacts.ResolvedDependency;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.io.Serializable;
import java.util.Objects;

public abstract class IvyDummyRepositoryReference implements ConfigurableDSLElement<IvyDummyRepositoryReference>, RepositoryReference, Serializable, ProjectAssociatedDSLElement {
    private static final long serialVersionUID = 8472300128115908221L;

    private transient final Project project;
    private final String group;
    private final String name;
    private final String version;
    private final String classifier;
    private final String extension;

    @Inject
    public IvyDummyRepositoryReference(@NotNull Project project, String group, String name, String version, String classifier, String extension) {
        this.project = project;
        this.group = group;
        this.name = name;
        this.version = version;
        this.classifier = classifier.isEmpty() ? null : classifier;
        this.extension = extension.isEmpty() ? null : extension;
    }

    @Override
    public Project getProject() {
        return project;
    }

    @Override
    public String getGroup() {
        return group;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public String getClassifier() {
        return classifier;
    }

    @Override
    public String getExtension() {
        return extension;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        final IvyDummyRepositoryReference that = (IvyDummyRepositoryReference) obj;
        return Objects.equals(this.group, that.group) &&
                Objects.equals(this.name, that.name) &&
                Objects.equals(this.version, that.version) &&
                Objects.equals(this.classifier, that.classifier) &&
                Objects.equals(this.extension, that.extension);
    }

    @Override
    public int hashCode() {
        return Objects.hash(group, name, version, classifier, extension);
    }

    @Override
    public String toString() {
        return "IvyDummyRepositoryEntryDependency[" +
                "group=" + group + ", " +
                "name=" + name + ", " +
                "version=" + version + ", " +
                "classifier=" + classifier + ", " +
                "extension=" + extension + ']';
    }

    public static abstract class Builder extends GroovyObjectSupport implements RepositoryReference.Builder<Builder, IvyDummyRepositoryReference> {
        private final Project project;
        private String group;
        private String name;
        private String version;
        private String classifier;
        private String extension;

        @Inject
        public Builder(Project project) {
            this.project = project;
        }

        public static Builder create(Project project) {
            return project.getObjects().newInstance(Builder.class, project);
        }

        @Override
        public Project getProject() {
            return project;
        }

        public String getGroup() {
            return group;
        }

        public String getName() {
            return name;
        }

        public String getVersion() {
            return version;
        }

        public String getClassifier() {
            return classifier;
        }

        public String getExtension() {
            return extension;
        }

        @Override
        public Builder setGroup(String group) {
            this.group = group;
            return this;
        }

        @Override
        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        @Override
        public Builder setVersion(String version) {
            this.version = version;
            return this;
        }

        @Override
        public Builder setClassifier(String classifier) {
            this.classifier = classifier;
            return this;
        }

        @Override
        public Builder setExtension(String extension) {
            this.extension = extension;
            return this;
        }

        @Override
        public Builder from(ModuleDependency externalModuleDependency) {
            setGroup(externalModuleDependency.getGroup());
            setName(externalModuleDependency.getName());
            setVersion(externalModuleDependency.getVersion());

            if (!externalModuleDependency.getArtifacts().isEmpty()) {
                final DependencyArtifact artifact = externalModuleDependency.getArtifacts().iterator().next();
                setClassifier(artifact.getClassifier());
                setExtension(artifact.getExtension());
            }
            return this;
        }

        @Override
        public Builder from(ResolvedDependency externalModuleDependency) {
            setGroup(externalModuleDependency.getModuleGroup());
            setName(externalModuleDependency.getModuleName());
            setVersion(externalModuleDependency.getModuleVersion());
            setExtension(ResolvedDependencyUtils.getExtension(externalModuleDependency));
            setClassifier(ResolvedDependencyUtils.getClassifier(externalModuleDependency));
            return this;
        }

        @Override
        public Builder but() {
            return create(project).setGroup(group).setName(name).setVersion(version).setClassifier(classifier).setExtension(extension);
        }

        public IvyDummyRepositoryReference build() {
            return project.getObjects().newInstance(IvyDummyRepositoryReference.class, project, group, name, version, classifier == null ? "" : classifier, extension == null ? "" : extension);
        }
    }
}
