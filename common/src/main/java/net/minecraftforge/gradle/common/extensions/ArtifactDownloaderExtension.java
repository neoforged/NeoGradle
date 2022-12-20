package net.minecraftforge.gradle.common.extensions;

import groovyjarjarantlr4.v4.runtime.misc.NotNull;
import net.minecraftforge.gradle.common.util.ConfigurableObject;
import net.minecraftforge.gradle.dsl.common.extensions.ArtifactDownloader;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ResolvedConfiguration;
import org.gradle.api.provider.Provider;

import javax.inject.Inject;
import java.io.File;

/**
 * Extension which handles multiple downloads of artifacts, including generating them if needed.
 * This is based of the MavenArtifactDownloader in FG5 but is now project specific.
 */
public abstract class ArtifactDownloaderExtension extends ConfigurableObject<ArtifactDownloader> implements ArtifactDownloader {

    private final Project project;

    @Inject
    public ArtifactDownloaderExtension(final Project project) {
        this.project = project;
    }

    @Override
    @NotNull
    public Provider<String> version(String notation) {
        return project.provider(() -> {
            final Configuration dummyConfiguration = project.getConfigurations().detachedConfiguration(project.getDependencies().create(notation));
            final ResolvedConfiguration resolvedConfiguration = dummyConfiguration.getResolvedConfiguration();
            return resolvedConfiguration.getResolvedArtifacts().iterator().next().getModuleVersion().getId().getVersion();
        });
    }

    @Override
    @NotNull
    public Provider<File> file(String notation) {
        return project.provider(() -> {
            final Configuration dummyConfiguration = project.getConfigurations().detachedConfiguration(project.getDependencies().create(notation));
            final ResolvedConfiguration resolvedConfiguration = dummyConfiguration.getResolvedConfiguration();
            return resolvedConfiguration.getResolvedArtifacts().iterator().next().getFile();
        });
    }
}
