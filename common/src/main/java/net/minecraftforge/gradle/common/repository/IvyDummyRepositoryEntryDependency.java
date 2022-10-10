package net.minecraftforge.gradle.common.repository;

import org.gradle.api.artifacts.DependencyArtifact;
import org.gradle.api.artifacts.ExternalModuleDependency;

import java.io.Serializable;

public record IvyDummyRepositoryEntryDependency(String group, String name, String version, String classifier, String extension) implements Serializable {
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

        public Builder but() {
            return create().withGroup(group).withName(name).withVersion(version).withClassifier(classifier).withExtension(extension);
        }

        public IvyDummyRepositoryEntryDependency build() {
            return new IvyDummyRepositoryEntryDependency(group, name, version, classifier, extension);
        }
    }
}
