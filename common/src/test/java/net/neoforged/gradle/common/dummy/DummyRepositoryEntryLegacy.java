package net.neoforged.gradle.common.dummy;

import com.google.common.collect.ImmutableSet;
import net.neoforged.gradle.dsl.common.extensions.repository.RepositoryEntryLegacy;
import net.neoforged.gradle.dsl.common.extensions.repository.RepositoryReference;
import net.neoforged.gradle.dsl.common.util.ModuleReference;
import net.neoforged.gradle.util.ModuleDependencyUtils;
import net.neoforged.gradle.util.ResolvedDependencyUtils;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.DependencyArtifact;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.artifacts.ResolvedDependency;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.plugins.ExtensionContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class DummyRepositoryEntryLegacy implements RepositoryEntryLegacy<DummyRepositoryEntryLegacy, DummyRepositoryDependency>, RepositoryEntryLegacy.Builder<DummyRepositoryEntryLegacy, DummyRepositoryDependency, DummyRepositoryDependency> {
    private Project project;
    private String group;
    private String name;
    private String version;
    private String classifier;
    private String extension;
    private Set<RepositoryReference> dependencies = new HashSet<>();

    public DummyRepositoryEntryLegacy(Project project) {
        this.project = project;
    }

    public DummyRepositoryEntryLegacy(Project project, String group, String name, String version, String classifier, String extension, Set<RepositoryReference> dependencies) {
        this.project = project;
        this.group = group;
        this.name = name;
        this.version = version;
        this.classifier = classifier;
        this.extension = extension;
        this.dependencies = new HashSet<>(dependencies);
    }

    @Override
    public Project getProject() {
        return project;
    }

    @Override
    public boolean matches(ModuleComponentIdentifier id) {
        return getFullGroup().equals(id.getGroup()) &&
                getName().equals(id.getModule()) &&
                getVersion().equals(id.getVersion());
    }

    @NotNull
    @Override
    public Dependency toGradle(Project project) {
        final ModuleDependency moduleDependency = mock(ModuleDependency.class);
        final DependencyArtifact artifact = mock(DependencyArtifact.class);
        when(moduleDependency.getGroup()).thenReturn(getFullGroup());
        when(moduleDependency.getName()).thenReturn(getName());
        when(moduleDependency.getVersion()).thenReturn(getVersion());
        when(moduleDependency.getArtifacts()).thenReturn(Collections.singleton(artifact));
        when(artifact.getName()).thenReturn(getName());
        when(artifact.getClassifier()).thenReturn(getClassifier());
        when(artifact.getExtension()).thenReturn(getExtension());
        return moduleDependency;
    }

    @NotNull
    @Override
    public DummyRepositoryEntryLegacy asSources() {
        return but().setClassifier("sources");
    }

    @NotNull
    @Override
    public Path buildArtifactPath(Path baseDir) throws IOException {
        final Path artifactPath = baseDir.resolve(buildArtifactPath());
        Files.createDirectories(artifactPath.getParent());
        return artifactPath;
    }

    @NotNull
    @Override
    public String buildArtifactPath() {
        final String fileName = getClassifier() == null || getClassifier().equals("") ?
                String.format("%s-%s.%s", getName(), getVersion(), getExtension()) :
                String.format("%s-%s-%s.%s", getName(), getVersion(), getClassifier(), getExtension());

        final String groupPath = getFullGroup().replace('.', '/') + '/';

        return String.format("%s%s/%s/%s", groupPath, getName(), getVersion(), fileName);
    }

    @NotNull
    @Override
    public String getFullGroup() {
        if (getGroup() == null) {
            return "dummy";
        }

        return String.format("%s.%s", "dummy", getGroup());
    }

    @NotNull
    @Override
    public String getGroup() {
        return group;
    }

    @NotNull
    @Override
    public String getName() {
        return name;
    }

    @NotNull
    @Override
    public String getVersion() {
        return version;
    }

    @Nullable
    @Override
    public String getClassifier() {
        return classifier;
    }

    @Nullable
    @Override
    public String getExtension() {
        return extension;
    }

    @NotNull
    @Override
    public ModuleReference toModuleReference() {
        return new ModuleReference(getGroup(), getName(), getVersion(), getExtension(), getClassifier());
    }

    @NotNull
    @Override
    public ImmutableSet<? extends RepositoryReference> getDependencies() {
        return ImmutableSet.copyOf(dependencies);
    }

    @NotNull
    @Override
    public DummyRepositoryEntryLegacy setGroup(@NotNull String group) {
        this.group = group;
        return this;
    }

    @NotNull
    @Override
    public DummyRepositoryEntryLegacy setName(@NotNull String name) {
        this.name = name;
        return this;
    }

    @NotNull
    @Override
    public DummyRepositoryEntryLegacy setVersion(@NotNull String version) {
        this.version = version;
        return this;
    }

    @NotNull
    @Override
    public DummyRepositoryEntryLegacy setClassifier(@Nullable String classifier) {
        this.classifier = classifier;
        return this;
    }

    @NotNull
    @Override
    public DummyRepositoryEntryLegacy setExtension(@Nullable String extension) {
        this.extension = extension;
        return this;
    }

    @NotNull
    @Override
    public DummyRepositoryEntryLegacy from(@NotNull ModuleDependency dependency) {
        setGroup(dependency.getGroup());
        setName(dependency.getName());
        setVersion(dependency.getVersion());
        setClassifier(ModuleDependencyUtils.getClassifier(dependency));
        setExtension(ModuleDependencyUtils.getExtension(dependency));
        return this;
    }

    @NotNull
    @Override
    public DummyRepositoryEntryLegacy from(@NotNull ResolvedDependency resolvedDependency) {
        setGroup(resolvedDependency.getModuleGroup());
        setName(resolvedDependency.getModuleName());
        setVersion(resolvedDependency.getModuleVersion());
        setClassifier(ResolvedDependencyUtils.getClassifier(resolvedDependency));
        setExtension(ResolvedDependencyUtils.getExtension(resolvedDependency));
        return this;
    }

    @NotNull
    @Override
    public DummyRepositoryEntryLegacy setDependencies(@NotNull Collection<? extends RepositoryReference> dummyRepositoryDependencies) {
        this.dependencies = new HashSet<>(dummyRepositoryDependencies);
        return this;
    }

    @NotNull
    @Override
    public DummyRepositoryEntryLegacy setDependencies(@NotNull RepositoryReference... dummyRepositoryDependencies) {
        this.dependencies = new HashSet<>(Arrays.asList(dummyRepositoryDependencies));
        return this;
    }

    @NotNull
    @Override
    public DummyRepositoryEntryLegacy withDependency(@NotNull Consumer<DummyRepositoryDependency> consumer) {
        final DummyRepositoryDependency dependency = new DummyRepositoryDependency(project);
        consumer.accept(dependency);
        this.dependencies.add(dependency);
        return this;
    }

    @NotNull
    @Override
    public DummyRepositoryEntryLegacy withProcessedDependency(@NotNull Consumer<DummyRepositoryEntryLegacy> consumer) {
        final DummyRepositoryEntryLegacy dependency = new DummyRepositoryEntryLegacy(project);
        consumer.accept(dependency);
        this.dependencies.add(dependency);
        return this;
    }

    @NotNull
    @Override
    public DummyRepositoryEntryLegacy but() {
        return new DummyRepositoryEntryLegacy(project, group, name, version, classifier, extension, dependencies);
    }

    @Override
    public ExtensionContainer getExtensions() {
        return mock(ExtensionContainer.class);
    }
}
