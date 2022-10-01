package net.minecraftforge.gradle.common;

import net.minecraftforge.gradle.common.extensions.ArtifactDownloaderExtension;
import net.minecraftforge.gradle.common.extensions.ArtifactProviderExtension;
import net.minecraftforge.gradle.common.extensions.IvyDummyRepositoryExtension;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.plugins.ide.eclipse.EclipsePlugin;
import org.gradle.plugins.ide.idea.IdeaPlugin;
import org.jetbrains.gradle.ext.IdeaExtPlugin;

public class CommonProjectPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        project.getPluginManager().apply(JavaPlugin.class);
        // Apply both the idea and eclipse IDE plugins
        project.getPluginManager().apply(IdeaPlugin.class);
        project.getPluginManager().apply(IdeaExtPlugin.class);
        project.getPluginManager().apply(EclipsePlugin.class);

        project.getExtensions().create("artifactProviders", ArtifactProviderExtension.class, project);
        project.getExtensions().create("downloader", ArtifactDownloaderExtension.class, project);
        project.getExtensions().create("ivyDummyRepository", IvyDummyRepositoryExtension.class, project);
    }
}
