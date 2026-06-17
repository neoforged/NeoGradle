package net.neoforged.gradle.common.extensions;

import net.neoforged.gradle.common.enumextensions.EnumExtensionsPublishing;
import net.neoforged.gradle.common.extensions.problems.IProblemReporter;
import net.neoforged.gradle.dsl.common.extensions.EnumExtensions;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ConfigurablePublishArtifact;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.dsl.ArtifactHandler;
import org.gradle.api.artifacts.dsl.DependencyHandler;

import javax.inject.Inject;

public abstract class EnumExtensionsExtension implements EnumExtensions {
    private transient final DependencyHandler projectDependencies;
    private transient final ArtifactHandler projectArtifacts;

    private final Project project;

    @Inject
    public EnumExtensionsExtension(final Project project) {
        this.project = project;

        this.projectDependencies = project.getDependencies();
        this.projectArtifacts = project.getArtifacts();
    }

    @Override
    public Project getProject() {
        return project;
    }

    @Override
    public void expose(Object path, Action<ConfigurablePublishArtifact> action) {
        getFiles().from(path);
        projectArtifacts.add(EnumExtensionsPublishing.ENUM_EXTENSIONS_ELEMENTS_CONFIGURATION, path, action);
    }

    @Override
    public void expose(Object path) {
        expose(path, artifacts -> {
        });
    }

    @SuppressWarnings("removal")
    @Override
    public void expose(Dependency dependency) {
        project.getExtensions().getByType(IProblemReporter.class)
                .reporting(
                    spec -> spec.id("enum-extensions", "expose.deprecated")
                        .details("Using the expose(Dependency) method is deprecated.")
                        .solution("Use the dependency collectors: 'consume' and 'consumeApi' for adding enum extensions from your dependencies.")
                        .section("userdev-enum-extensions-from-dependencies")
                        .contextualLabel("Deprecations"),
                    project.getLogger()
                );

        projectDependencies.add(EnumExtensionsPublishing.ENUM_EXTENSIONS_API_CONFIGURATION, dependency);
    }
}
