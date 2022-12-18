package net.minecraftforge.gradle.common;

import net.minecraftforge.gradle.common.deobfuscation.DependencyDeobfuscator;
import net.minecraftforge.gradle.common.extensions.*;
import net.minecraftforge.gradle.common.extensions.dependency.replacement.DependencyReplacementExtension;
import net.minecraftforge.gradle.common.extensions.dependency.replacement.DependencyReplacementResult;
import net.minecraftforge.gradle.common.runtime.extensions.CommonRuntimeExtension;
import net.minecraftforge.gradle.common.runtime.naming.OfficialNamingChannelConfigurator;
import net.minecraftforge.gradle.common.tasks.DisplayMappingsLicenseTask;
import net.minecraftforge.gradle.common.util.GradleInternalUtils;
import net.minecraftforge.gradle.common.util.Utils;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;
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

        project.getExtensions().create("downloader", ArtifactDownloaderExtension.class, project);
        project.getExtensions().create("ivyDummyRepository", IvyDummyRepositoryExtension.class, project);
        project.getExtensions().create("minecraftCache", MinecraftArtifactCacheExtension.class, project);
        project.getExtensions().create("dependencyReplacements", DependencyReplacementExtension.class, project);

        project.getExtensions().create("minecraft", MinecraftExtension.class, project);
        project.getExtensions().create("mappings", MappingsExtension.class, project, project.getExtensions().getByType(MinecraftExtension.class));

        project.getExtensions().create("obfuscation", ObfuscationExtension.class, project);

        OfficialNamingChannelConfigurator.getInstance().configure(project);

        project.getTasks().create("handleNamingLicense", DisplayMappingsLicenseTask.class);

        project.getRepositories().maven(e -> {
            e.setUrl(Utils.MOJANG_MAVEN);
            e.metadataSources(MavenArtifactRepository.MetadataSources::artifact);
        });

        DependencyDeobfuscator.getInstance().apply(project);

        project.afterEvaluate(this::applyAfterEvaluate);
    }

    private void applyAfterEvaluate(final Project project) {
        GradleInternalUtils.getExtensions(project.getExtensions())
                .stream()
                .filter(CommonRuntimeExtension.class::isInstance)
                .map(extension -> (CommonRuntimeExtension<?,?,?>) extension)
                .forEach(CommonRuntimeExtension::bakeDefinitions);
    }
}
