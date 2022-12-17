package net.minecraftforge.gradle.common.repository;

import net.minecraftforge.gradle.common.util.ResolvedDependencyUtils;
import org.gradle.api.artifacts.DependencyArtifact;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.artifacts.ResolvedDependency;

import java.io.Serializable;
import java.util.Objects;

public final class IvyDummyRepositoryEntryDependency implements Serializable {
    private static final long serialVersionUID = 8472300128115908221L;
    private final String group;
    private final String name;
    private final String version;
    private final String classifier;
    private final String extension;

    public IvyDummyRepositoryEntryDependency(String group, String name, String version, String classifier, String extension) {
        this.group = group;
        this.name = name;
        this.version = version;
        this.classifier = classifier;
        this.extension = extension;
    }

    public String group() {
        return group;
    }

    public String name() {
        return name;
    }

    public String version() {
        return version;
    }

    public String classifier() {
        return classifier;
    }

    public String extension() {
        return extension;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        final IvyDummyRepositoryEntryDependency that = (IvyDummyRepositoryEntryDependency) obj;
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

    public static final class Builder {
        private String group;
        private String name;
        private String version;
        private String classifier;
        private String extension;

        private Builder() {
        }

        public static Builder create() {
            return new Builder();
        }

        public Builder withGroup(String group) {
            this.group = group;
            return this;
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withVersion(String version) {
            this.version = version;
            return this;
        }

        public Builder withClassifier(String classifier) {
            this.classifier = classifier;
            return this;
        }

        public Builder withExtension(String extension) {
            this.extension = extension;
            return this;
        }

        public void from(ExternalModuleDependency externalModuleDependency) {
            withGroup(externalModuleDependency.getGroup());
            withName(externalModuleDependency.getName());
            withVersion(externalModuleDependency.getVersion());

            if (!externalModuleDependency.getArtifacts().isEmpty()) {
                final DependencyArtifact artifact = externalModuleDependency.getArtifacts().iterator().next();
                withClassifier(artifact.getClassifier());
                withExtension(artifact.getExtension());
            }
        }

        public void from(ResolvedDependency externalModuleDependency) {
            withGroup(externalModuleDependency.getModuleGroup());
            withName(externalModuleDependency.getModuleName());
            withVersion(externalModuleDependency.getModuleVersion());
            withExtension(ResolvedDependencyUtils.getExtension(externalModuleDependency));
            withClassifier(ResolvedDependencyUtils.getClassifier(externalModuleDependency));
        }

        public Builder but() {
            return create().withGroup(group).withName(name).withVersion(version).withClassifier(classifier).withExtension(extension);
        }

        public IvyDummyRepositoryEntryDependency build() {
            return new IvyDummyRepositoryEntryDependency(group, name, version, classifier, extension);
        }
    }
}
