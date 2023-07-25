package net.neoforged.gradle.common.dummy;

import net.neoforged.gradle.dsl.common.extensions.repository.RepositoryReference;
import net.neoforged.gradle.dsl.common.util.ModuleReference;
import net.neoforged.gradle.util.ResolvedDependencyUtils;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.DependencyArtifact;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.artifacts.ResolvedDependency;
import org.gradle.api.plugins.ExtensionContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.mockito.Mockito.mock;

@SuppressWarnings("DataFlowIssue")
public final class DummyRepositoryDependency implements RepositoryReference, RepositoryReference.Builder<DummyRepositoryDependency, DummyRepositoryDependency> {

    private Project project;
    private String group;
    private String name;
    private String version;
    private String classifier;
    private String extension;

    public DummyRepositoryDependency(Project project) {
        this.project = project;
    }

    public DummyRepositoryDependency(Project project, String group, String name, String version, String classifier, String extension) {
        this.project = project;
        this.group = group;
        this.name = name;
        this.version = version;
        this.classifier = classifier;
        this.extension = extension;
    }

    @Override
    public Project getProject() {
        return project;
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
    public Dependency toGradle(Project project) {
        return project.getDependencies().create(toModuleReference().toString());
    }

    @NotNull
    @Override
    public DummyRepositoryDependency setGroup(@NotNull String group) {
        this.group = group;
        return this;
    }

    @NotNull
    @Override
    public DummyRepositoryDependency setName(@NotNull String name) {
        this.name = name;
        return this;
    }

    @NotNull
    @Override
    public DummyRepositoryDependency setVersion(@NotNull String version) {
        this.version = version;
        return this;
    }

    @NotNull
    @Override
    public DummyRepositoryDependency setClassifier(@Nullable String classifier) {
        this.classifier = classifier;
        return this;
    }

    @NotNull
    @Override
    public DummyRepositoryDependency setExtension(@Nullable String extension) {
        this.extension = extension;
        return this;
    }

    @Override
    public DummyRepositoryDependency from(ModuleDependency externalModuleDependency) {
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
    public DummyRepositoryDependency from(ResolvedDependency externalModuleDependency) {
        setGroup(externalModuleDependency.getModuleGroup());
        setName(externalModuleDependency.getModuleName());
        setVersion(externalModuleDependency.getModuleVersion());
        setExtension(ResolvedDependencyUtils.getExtension(externalModuleDependency));
        setClassifier(ResolvedDependencyUtils.getClassifier(externalModuleDependency));
        return this;
    }

    @NotNull
    @Override
    public DummyRepositoryDependency but() {
        return new DummyRepositoryDependency(project, group, name, version, classifier, extension);
    }

    @Override
    public ExtensionContainer getExtensions() {
        return mock(ExtensionContainer.class);
    }
}
