package net.minecraftforge.gradle.common.repository;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * An entry which can potentially be requested by IDEs which interface with gradle.
 *
 * @param group The group of the dependency.
 * @param name The name of the dependency.
 * @param version The version of the dependency.
 * @param classifier The classifier of the dependency.
 * @param extension The extension of the dependency.
 */
public record IvyDummyRepositoryEntry(String group, String name, String version, String classifier, String extension) implements Serializable {

    private static final String FG_DUMMY_FG_MARKER = "fg_dummy_fg";

    public String fullGroup() {
        if (group() == null) {
            return FG_DUMMY_FG_MARKER;
        }

        return "%s.%s".formatted(FG_DUMMY_FG_MARKER, group());
    }

    public boolean matches(ModuleComponentIdentifier id) {
        return fullGroup().equals(id.getGroup()) &&
                name().equals(id.getModule()) &&
                version().equals(id.getVersion());
    }

    public Dependency asDependency(Project project) {
        return project.getDependencies().create(toString());
    }

    public Path artifactPath(final Path baseDir) throws IOException {
        final Path artifactPath = baseDir.resolve(artifactPath());
        Files.createDirectories(artifactPath.getParent());
        return artifactPath;
    }

    public String artifactPath() {
        final String fileName = classifier() == null || classifier().equals("") ?
                "%s-%s.%s".formatted(name(), version(), extension()) :
                "%s-%s-%s.%s".formatted(name(), version(), classifier(), extension());

        final String groupPath = fullGroup().replace('.', '/') + '/';

        return "%s%s/%s/%s".formatted(groupPath, name(), version(), fileName);
    }

    public IvyDummyRepositoryEntry asSources() {
        return IvyDummyRepositoryEntry.Builder.create(this).withClassifier("sources").build();
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append(fullGroup());
        builder.append(':');
        builder.append(name());
        builder.append(":");
        builder.append(version());

        if (classifier() != null && !classifier().equals("")) {
            builder.append("-");
            builder.append(classifier());
        }

        if (extension() != null && !extension().equalsIgnoreCase("jar")) {
            builder.append("@");
            builder.append(extension());
        }

        return builder.toString();
    }

    public static final class Builder {
        private String group;
        private String name;
        private String version;
        private String classifier = "";
        private String extension = "jar";

        private Builder() {
        }

        public static Builder create() {
            return new Builder();
        }

        public static Builder create(IvyDummyRepositoryEntry entry) {
            return new Builder()
                    .withGroup(entry.group())
                    .withName(entry.name())
                    .withVersion(entry.version())
                    .withClassifier(entry.classifier())
                    .withExtension(entry.extension());
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

        public Builder with(final ExternalModuleDependency dependency) {
            this.group = dependency.getGroup();
            this.name = dependency.getName();
            this.version = dependency.getVersion();
            return this;
        }

        public Builder but() {
            return create().withGroup(group).withName(name).withVersion(version).withClassifier(classifier).withExtension(extension);
        }

        public IvyDummyRepositoryEntry build() {
            return new IvyDummyRepositoryEntry(group, name, version, classifier, extension);
        }
    }
}
