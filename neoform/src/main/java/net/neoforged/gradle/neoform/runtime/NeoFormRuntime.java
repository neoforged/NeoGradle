package net.neoforged.gradle.neoform.runtime;

import net.neoforged.gradle.dsl.common.util.ConfigurationUtils;
import net.neoforged.gradle.dsl.neoform.configuration.NeoFormConfigConfigurationSpecV2;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.FileSystemLocation;
import org.gradle.api.file.FileTree;
import org.gradle.api.provider.Provider;

import java.io.File;
import java.util.Set;

public class NeoFormRuntime {

    private static final String NEOFORM_ARTIFACT = "net.neoforged:neoform:%s@zip";

    public static Provider<File> getNeoFormArchive(Project project, String version) {
        final String artifact = NEOFORM_ARTIFACT.formatted(version);

        final Configuration configuration = ConfigurationUtils.temporaryUnhandledConfiguration(
                project.getConfigurations(),
                "neoform%s".formatted(version),
                project.getDependencies().create(artifact)
        );

        return configuration.getIncoming().getArtifacts().getResolvedArtifacts().map(artifacts -> {
            if (artifacts.isEmpty()) {
                throw new IllegalStateException("Failed to resolve NeoForm artifact: %s".formatted(artifact));
            }

            return artifacts.iterator().next().getFile();
        });
    }

    public static Provider<NeoFormConfigConfigurationSpecV2> parseConfiguration(Provider<File> neoFormArchiveFile) {
        return neoFormArchiveFile.map(NeoFormConfigConfigurationSpecV2::getFromArchive);
    }

    public static Provider<String> getMinecraftVersion(Provider<NeoFormConfigConfigurationSpecV2> config) {
        return config.map(NeoFormConfigConfigurationSpecV2::getVersion);
    }
}
