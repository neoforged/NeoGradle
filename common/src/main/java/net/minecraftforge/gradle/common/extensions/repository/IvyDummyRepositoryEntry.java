package net.minecraftforge.gradle.common.extensions.repository;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import net.minecraftforge.gradle.base.util.ConfigurableObject;
import net.minecraftforge.gradle.dsl.common.extensions.repository.RepositoryEntry;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.DependencyArtifact;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedDependency;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;

import javax.inject.Inject;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/**
 * An entry which can potentially be requested by IDEs which interface with gradle.
 */
public abstract class IvyDummyRepositoryEntry extends ConfigurableObject<IvyDummyRepositoryEntry> implements RepositoryEntry<IvyDummyRepositoryEntry, IvyDummyRepositoryReference>, Serializable {

    private static final String FG_DUMMY_FG_MARKER = "fg_dummy_fg";
    private static final long serialVersionUID = 4025734172533096653L;

    private transient final Project project;
    private final String group;
    private final String name;
    private final String version;
    private final String classifier;
    private final String extension;
    private final Collection<IvyDummyRepositoryReference> dependencies;

    /**
     * @param project      The project thsi entry resides in.
     * @param group        The group of the dependency.
     * @param name         The name of the dependency.
     * @param version      The version of the dependency.
     * @param classifier   The classifier of the dependency.
     * @param extension    The extension of the dependency.
     * @param dependencies The dependencies for this entry.
     */
    @Inject
    public IvyDummyRepositoryEntry(Project project, String group, String name, String version, String classifier, String extension, Collection<IvyDummyRepositoryReference> dependencies) {
        this.project = project;
        this.group = group;
        this.name = name;
        this.version = version;
        this.classifier = classifier;
        this.extension = extension;
        this.dependencies = dependencies;
    }

    @Override
    public Project getProject() {
        return project;
    }

    @Override
    public String getFullGroup() {
        if (getGroup() == null) {
            return FG_DUMMY_FG_MARKER;
        }

        return String.format("%s.%s", FG_DUMMY_FG_MARKER, getGroup());
    }

    @Override
    public boolean matches(ModuleComponentIdentifier id) {
        return getFullGroup().equals(id.getGroup()) &&
                getName().equals(id.getModule()) &&
                getVersion().equals(id.getVersion());
    }

    @Override
    public Dependency toGradle(Project project) {
        return project.getDependencies().create(toString());
    }

    @Override
    public IvyDummyRepositoryEntry asSources() {
        return Builder.create(project, this).setClassifier("sources").build();
    }

    @Override
    public Path buildArtifactPath(final Path baseDir) throws IOException {
        final Path artifactPath = baseDir.resolve(buildArtifactPath());
        Files.createDirectories(artifactPath.getParent());
        return artifactPath;
    }

    @Override
    public String buildArtifactPath() {
        final String fileName = getClassifier() == null || getClassifier().equals("") ?
                String.format("%s-%s.%s", getName(), getVersion(), getExtension()) :
                String.format("%s-%s-%s.%s", getName(), getVersion(), getClassifier(), getExtension());

        final String groupPath = getFullGroup().replace('.', '/') + '/';

        return String.format("%s%s/%s/%s", groupPath, getName(), getVersion(), fileName);
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
    public Collection<IvyDummyRepositoryReference> getDependencies() {
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
        final StringBuilder builder = new StringBuilder();

        final String group = getFullGroup();
        final String artifactName = getName();
        final String version = getVersion();
        final String extension = getExtension();
        final String classifier = getClassifier();

        if (!group.trim().isEmpty()) {
            builder.append(group);
        }

        builder.append(":");
        builder.append(artifactName);

        builder.append(":");
        builder.append(version);

        if (classifier != null && !classifier.trim().isEmpty()) {
            builder.append(":");
            builder.append(classifier);
        }

        if (extension != null && !extension.trim().isEmpty() && !extension.trim().toLowerCase(Locale.ROOT).equals("jar")) {
            builder.append("@")
                    .append(extension);
        }

        return builder.toString();
    }

    public static abstract class Builder extends ConfigurableObject<Builder> implements RepositoryEntry.Builder<Builder, IvyDummyRepositoryReference, IvyDummyRepositoryReference.Builder> {
        private final Project project;
        private String group;
        private String name;
        private String version;
        private String classifier = "";
        private String extension = "jar";
        private final Set<IvyDummyRepositoryReference> dependencies = Sets.newHashSet();

        @Inject
        public Builder(Project project) {
            this.project = project;
        }

        public static Builder create(Project project) {
            return project.getObjects().newInstance(Builder.class, project);
        }

        public static Builder create(Project project, IvyDummyRepositoryEntry entry) {
            return create(project)
                    .setGroup(entry.getGroup())
                    .setName(entry.getName())
                    .setVersion(entry.getVersion())
                    .setClassifier(entry.getClassifier())
                    .setExtension(entry.getExtension())
                    .setDependencies(entry.getDependencies());
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
        public ImmutableSet<IvyDummyRepositoryReference> getDependencies() {
            return ImmutableSet.copyOf(dependencies);
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
        public Builder from(final ModuleDependency dependency) {
            setGroup(dependency.getGroup());
            setName(dependency.getName());
            setVersion(dependency.getVersion());

            if (!dependency.getArtifacts().isEmpty()) {
                final DependencyArtifact artifact = dependency.getArtifacts().iterator().next();
                setClassifier(artifact.getClassifier());
                setExtension(artifact.getExtension());
            }
            return this;
        }

        @Override
        public Builder from(ResolvedDependency resolvedDependency) {
            setGroup(resolvedDependency.getModuleGroup());
            setName(resolvedDependency.getModuleName());
            setVersion(resolvedDependency.getModuleVersion());

            if (resolvedDependency.getModuleArtifacts().size() > 0) {
                final ResolvedArtifact artifact = resolvedDependency.getModuleArtifacts().iterator().next();
                setClassifier(artifact.getClassifier());
                setExtension(artifact.getExtension());
            }
            return this;
        }

        @Override
        public Builder setDependencies(final Collection<IvyDummyRepositoryReference> dependencies) {
            this.dependencies.addAll(dependencies);
            return this;
        }

        @Override
        public Builder setDependencies(final IvyDummyRepositoryReference... dependencies) {
            this.dependencies.addAll(Arrays.asList(dependencies));
            return this;
        }

        @Override
        public Builder withDependency(final Consumer<IvyDummyRepositoryReference.Builder> consumer) {
            final IvyDummyRepositoryReference.Builder builder = IvyDummyRepositoryReference.Builder.create(project);
            consumer.accept(builder);
            this.dependencies.add(builder.build());
            return this;
        }

        @Override
        public Builder but() {
            return create(project).setGroup(group).setName(name).setVersion(version).setClassifier(classifier).setExtension(extension).setDependencies(dependencies);
        }

        public IvyDummyRepositoryEntry build() {
            return project.getObjects().newInstance(IvyDummyRepositoryEntry.class, project, group, name, version, classifier, extension, dependencies);
        }
    }
}
