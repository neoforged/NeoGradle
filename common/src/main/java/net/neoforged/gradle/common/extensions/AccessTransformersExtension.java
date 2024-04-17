package net.neoforged.gradle.common.extensions;

import net.neoforged.gradle.common.CommonProjectPlugin;
import net.neoforged.gradle.common.extensions.base.BaseFilesWithEntriesExtension;
import net.neoforged.gradle.dsl.common.extensions.AccessTransformers;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ConfigurablePublishArtifact;
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
    public void consume(Object notation) {
        this.projectDependencies.add(CommonProjectPlugin.ACCESS_TRANSFORMER_CONFIGURATION, notation);
    }

    @Override
    public void consumeApi(Object notation) {
        this.projectDependencies.add(CommonProjectPlugin.ACCESS_TRANSFORMER_API_CONFIGURATION, notation);
    }

    @Override
    public void expose(Object path, Action<ConfigurablePublishArtifact> action) {
        projectArtifacts.add(CommonProjectPlugin.ACCESS_TRANSFORMER_ELEMENTS_CONFIGURATION, path, action);
    }

    @Override
    public void expose(Object path) {
        expose(path, artifacts -> {});
    }
}
