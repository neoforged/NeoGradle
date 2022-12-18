package net.minecraftforge.gradle.common.repository;

import com.google.common.collect.Sets;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.DependencyArtifact;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedDependency;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;

/**
 * An entry which can potentially be requested by IDEs which interface with gradle.
 */
public final class IvyDummyRepositoryEntry implements Serializable {

    private static final String FG_DUMMY_FG_MARKER = "fg_dummy_fg";
    private static final long serialVersionUID = 4025734172533096653L;
    private final String group;
    private final String name;
    private final String version;
    private final String classifier;
    private final String extension;
    private final Collection<IvyDummyRepositoryEntryDependency> dependencies;

    /**
     * @param group        The group of the dependency.
     * @param name         The name of the dependency.
     * @param version      The version of the dependency.
     * @param classifier   The classifier of the dependency.
     * @param extension    The extension of the dependency.
     * @param dependencies The dependencies for this entry.
     */
    public IvyDummyRepositoryEntry(String group, String name, String version, String classifier, String extension, Collection<IvyDummyRepositoryEntryDependency> dependencies) {
        this.group = group;
        this.name = name;
        this.version = version;
        this.classifier = classifier;
        this.extension = extension;
        this.dependencies = dependencies;
    }

    public IvyDummyRepositoryEntry(String group, String name, String version, String classifier, String extension) {
        this(group, name, version, classifier, extension, Collections.emptyList());
    }

    public String fullGroup() {
        if (group() == null) {
            return FG_DUMMY_FG_MARKER;
        }

        return String.format("%s.%s", FG_DUMMY_FG_MARKER, group());
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
                String.format("%s-%s.%s", name(), version(), extension()) :
                String.format("%s-%s-%s.%s", name(), version(), classifier(), extension());

        final String groupPath = fullGroup().replace('.', '/') + '/';

        return String.format("%s%s/%s/%s", groupPath, name(), version(), fileName);
    }

    public IvyDummyRepositoryEntry asSources() {
        return Builder.create(this).withClassifier("sources").build();
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

    public Collection<IvyDummyRepositoryEntryDependency> dependencies() {
        return dependencies;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        final IvyDummyRepositoryEntry that = (IvyDummyRepositoryEntry) obj;
        return Objects.equals(this.group, that.group) &&
                Objects.equals(this.name, that.name) &&
                Objects.equals(this.version, that.version) &&
                Objects.equals(this.classifier, that.classifier) &&
                Objects.equals(this.extension, that.extension) &&
                Objects.equals(this.dependencies, that.dependencies);
    }

    @Override
    public int hashCode() {
        return Objects.hash(group, name, version, classifier, extension, dependencies);
    }

    @Override
    public String toString() {
        return "IvyDummyRepositoryEntry{" +
                "group='" + group + '\'' +
                ", name='" + name + '\'' +
                ", version='" + version + '\'' +
                ", classifier='" + classifier + '\'' +
                ", extension='" + extension + '\'' +
                '}';
    }

    public static final class Builder {
        private String group;
        private String name;
        private String version;
        private String classifier = "";
        private String extension = "jar";
        private Set<IvyDummyRepositoryEntryDependency> dependencies = Sets.newHashSet();

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
                    .withExtension(entry.extension())
                    .withDependencies(entry.dependencies());
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

        public Builder from(final ModuleDependency dependency) {
            withGroup(dependency.getGroup());
            withName(dependency.getName());
            withVersion(dependency.getVersion());

            if (!dependency.getArtifacts().isEmpty()) {
                final DependencyArtifact artifact = dependency.getArtifacts().iterator().next();
                withClassifier(artifact.getClassifier());
                withExtension(artifact.getExtension());
            }
            return this;
        }

        public void from(ResolvedDependency resolvedDependency) {
            withGroup(resolvedDependency.getModuleGroup());
            withName(resolvedDependency.getModuleName());
            withVersion(resolvedDependency.getModuleVersion());

            if (resolvedDependency.getModuleArtifacts().size() > 0) {
                final ResolvedArtifact artifact = resolvedDependency.getModuleArtifacts().iterator().next();
                withClassifier(artifact.getClassifier());
                withExtension(artifact.getExtension());
            }
        }

        public Builder withDependencies(final Collection<IvyDummyRepositoryEntryDependency> dependencies) {
            this.dependencies.addAll(dependencies);
            return this;
        }

        public Builder withDependencies(final IvyDummyRepositoryEntryDependency... dependencies) {
            this.dependencies.addAll(Arrays.asList(dependencies));
            return this;
        }

        public Builder withDependency(final Consumer<IvyDummyRepositoryEntryDependency.Builder> consumer) {
            final IvyDummyRepositoryEntryDependency.Builder builder = IvyDummyRepositoryEntryDependency.Builder.create();
            consumer.accept(builder);
            this.dependencies.add(builder.build());
            return this;
        }

        public Builder but() {
            return create().withGroup(group).withName(name).withVersion(version).withClassifier(classifier).withExtension(extension).withDependencies(dependencies);
        }

        public IvyDummyRepositoryEntry build() {
            return new IvyDummyRepositoryEntry(group, name, version, classifier, extension, dependencies);
        }
    }
}
