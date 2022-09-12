package net.minecraftforge.gradle.common;

import net.minecraftforge.gradle.common.extensions.ArtifactDownloaderExtension;
import net.minecraftforge.gradle.common.extensions.ArtifactProviderExtension;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class CommonPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        project.getExtensions().create("artifactProviders", ArtifactProviderExtension.class);
        project.getExtensions().create("downloader", ArtifactDownloaderExtension.class);
    }
}
