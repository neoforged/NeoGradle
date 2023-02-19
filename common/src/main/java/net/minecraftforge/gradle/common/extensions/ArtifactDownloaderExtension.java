package net.minecraftforge.gradle.common.extensions;

import groovyjarjarantlr4.v4.runtime.misc.NotNull;
import net.minecraftforge.gradle.base.util.ConfigurableObject;
import net.minecraftforge.gradle.dsl.common.extensions.ArtifactDownloader;
import org.apache.ivy.plugins.repository.Repository;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ResolvedConfiguration;
import org.gradle.api.artifacts.repositories.ArtifactRepository;
import org.gradle.api.provider.Provider;
import org.gradle.internal.impldep.it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import org.gradle.internal.impldep.it.unimi.dsi.fastutil.objects.Object2ObjectSortedMaps;

import javax.inject.Inject;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

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
    public Project getProject() {
        return project;
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
            final Map<String, ArtifactRepository> repositories = new LinkedHashMap<>(project.getRepositories().getAsMap());
            project.getRepositories().clear();

            try {
                for (final ArtifactRepository repository : repositories.values()) {
                    project.getRepositories().add(repository);
                    final Configuration dummyConfiguration = project.getConfigurations().detachedConfiguration(project.getDependencies().create(notation));
                    final ResolvedConfiguration resolvedConfiguration = dummyConfiguration.getResolvedConfiguration();
                    final Set<File> result = resolvedConfiguration.getLenientConfiguration().getFiles();
                    if (result.size() >= 1) {
                        return result.iterator().next();
                    }
                }
            }
            finally {
                project.getRepositories().addAll(repositories.values());
            }

            throw new RuntimeException("Could not find artifact " + notation);
        });
    }
}
