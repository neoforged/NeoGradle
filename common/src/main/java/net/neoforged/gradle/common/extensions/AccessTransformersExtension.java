package net.neoforged.gradle.common.extensions;

import net.neoforged.gradle.common.accesstransformers.AccessTransformerPublishing;
import net.neoforged.gradle.common.extensions.problems.IProblemReporter;
import net.neoforged.gradle.dsl.common.extensions.AccessTransformers;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ConfigurablePublishArtifact;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.dsl.ArtifactHandler;
import org.gradle.api.artifacts.dsl.DependencyHandler;

import javax.inject.Inject;

public abstract class AccessTransformersExtension implements AccessTransformers {
    private transient final DependencyHandler projectDependencies;
    private transient final ArtifactHandler projectArtifacts;

    private final Project project;

    @SuppressWarnings("UnstableApiUsage")
    @Inject
    public AccessTransformersExtension(Project project) {
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
        projectArtifacts.add(AccessTransformerPublishing.ACCESS_TRANSFORMER_ELEMENTS_CONFIGURATION, path, action);
    }

    @Override
    public void expose(Object path) {
        expose(path, artifacts -> {});
    }

    @Override
    public void expose(Dependency dependency) {
        project.getExtensions().getByType(IProblemReporter.class)
            .reporting(
                spec -> spec.id("access-transformers", "expose.deprecated")
                    .details("Using the expose(Dependency) method is deprecated.")
                    .solution("Use the dependency collectors: 'consume' and 'consumeApi' for adding access transformers from your dependencies.")
                    .section("userdev-access-transformers-from-dependencies")
                    .contextualLabel("Deprecations"),
                project.getLogger()
            );

        projectDependencies.add(AccessTransformerPublishing.ACCESS_TRANSFORMER_API_CONFIGURATION, dependency);
    }
}
