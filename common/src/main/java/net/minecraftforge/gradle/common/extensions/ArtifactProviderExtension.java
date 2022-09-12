package net.minecraftforge.gradle.common.extensions;

import groovyjarjarantlr4.v4.runtime.misc.NotNull;
import net.minecraftforge.gradle.common.util.Artifact;
import org.gradle.api.Project;
import org.gradle.api.provider.Provider;

import javax.inject.Inject;

public abstract class ArtifactProviderExtension {

    private final Project project;

    @Inject
    public ArtifactProviderExtension(Project project) {
        this.project = project;
    }

    @NotNull
    public Provider<Artifact> from(final String artifactNotation) {
        return project.provider(() -> Artifact.from(artifactNotation));
    }
}
