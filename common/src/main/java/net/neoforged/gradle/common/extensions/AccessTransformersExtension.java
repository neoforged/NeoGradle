package net.neoforged.gradle.common.extensions;

import net.neoforged.gradle.common.CommonProjectPlugin;
import net.neoforged.gradle.common.extensions.base.BaseFilesWithEntriesExtension;
import net.neoforged.gradle.dsl.common.extensions.AccessTransformers;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ConfigurablePublishArtifact;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.dsl.ArtifactHandler;
import org.gradle.api.artifacts.dsl.DependencyHandler;

import javax.inject.Inject;

public abstract class AccessTransformersExtension extends BaseFilesWithEntriesExtension<AccessTransformers> implements AccessTransformers {
    private transient final DependencyHandler projectDependencies;
    private transient final ArtifactHandler projectArtifacts;

    @Inject
    public AccessTransformersExtension(Project project) {
        super(project);

        this.projectDependencies = project.getDependencies();
        this.projectArtifacts = project.getArtifacts();
    }

    @Override
    public Dependency consume(Object notation) {
        return this.projectDependencies.add(CommonProjectPlugin.ACCESS_TRANSFORMER_CONFIGURATION, notation);
    }

    @Override
    public Dependency consumeApi(Object notation) {
        return this.projectDependencies.add(CommonProjectPlugin.ACCESS_TRANSFORMER_API_CONFIGURATION, notation);
    }

    @Override
    public void expose(Object path, Action<ConfigurablePublishArtifact> action) {
        file(path);
        projectArtifacts.add(CommonProjectPlugin.ACCESS_TRANSFORMER_ELEMENTS_CONFIGURATION, path, action);
    }

    @Override
    public void expose(Object path) {
        expose(path, artifacts -> {});
    }

    @Override
    public void expose(Dependency dependency) {
        projectDependencies.add(CommonProjectPlugin.ACCESS_TRANSFORMER_API_CONFIGURATION, dependency);
    }
}
