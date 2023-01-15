package net.minecraftforge.gradle.common;

import net.minecraftforge.gradle.base.util.GradleInternalUtils;
import net.minecraftforge.gradle.common.deobfuscation.DependencyDeobfuscator;
import net.minecraftforge.gradle.common.extensions.AccessTransformersExtension;
import net.minecraftforge.gradle.common.extensions.ArtifactDownloaderExtension;
import net.minecraftforge.gradle.common.extensions.MappingsExtension;
import net.minecraftforge.gradle.common.extensions.MinecraftArtifactCacheExtension;
import net.minecraftforge.gradle.common.extensions.MinecraftExtension;
import net.minecraftforge.gradle.common.extensions.SourceSetProjectExtension;
import net.minecraftforge.gradle.common.extensions.dependency.replacement.DependencyReplacementsExtension;
import net.minecraftforge.gradle.common.extensions.obfuscation.ObfuscationExtension;
import net.minecraftforge.gradle.common.extensions.repository.IvyDummyRepositoryExtension;
import net.minecraftforge.gradle.common.runtime.definition.CommonRuntimeDefinition;
import net.minecraftforge.gradle.common.runtime.extensions.CommonRuntimeExtension;
import net.minecraftforge.gradle.common.runtime.naming.OfficialNamingChannelConfigurator;
import net.minecraftforge.gradle.common.tasks.DisplayMappingsLicenseTask;
import net.minecraftforge.gradle.common.util.TaskDependencyUtils;
import net.minecraftforge.gradle.common.util.exceptions.MultipleDefinitionsFoundException;
import net.minecraftforge.gradle.dsl.common.extensions.AccessTransformers;
import net.minecraftforge.gradle.dsl.common.extensions.ArtifactDownloader;
import net.minecraftforge.gradle.dsl.common.extensions.Mappings;
import net.minecraftforge.gradle.dsl.common.extensions.Minecraft;
import net.minecraftforge.gradle.dsl.common.extensions.MinecraftArtifactCache;
import net.minecraftforge.gradle.dsl.common.extensions.dependency.replacement.DependencyReplacement;
import net.minecraftforge.gradle.dsl.common.extensions.obfuscation.Obfuscation;
import net.minecraftforge.gradle.dsl.common.extensions.repository.Repository;
import net.minecraftforge.gradle.dsl.common.util.Constants;
import net.minecraftforge.gradle.dsl.runs.run.Runs;
import net.minecraftforge.gradle.runs.RunsPlugin;
import net.minecraftforge.gradle.runs.run.RunImpl;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.plugins.ide.eclipse.EclipsePlugin;
import org.gradle.plugins.ide.idea.IdeaPlugin;
import org.jetbrains.gradle.ext.IdeaExtPlugin;

import java.util.Collection;
import java.util.Map;

public class CommonProjectPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        project.getPluginManager().apply(JavaPlugin.class);

        // Apply both the idea and eclipse IDE plugins
        project.getPluginManager().apply(IdeaPlugin.class);
        project.getPluginManager().apply(IdeaExtPlugin.class);
        project.getPluginManager().apply(EclipsePlugin.class);

        // Setup runs
        project.getPluginManager().apply(RunsPlugin.class);

        project.getExtensions().create(ArtifactDownloader.class, "artifactDownloader", ArtifactDownloaderExtension.class, project);
        project.getExtensions().create(Repository.class, "ivyDummyRepository", IvyDummyRepositoryExtension.class, project);
        project.getExtensions().create(MinecraftArtifactCache.class, "minecraftArtifactCache", MinecraftArtifactCacheExtension.class, project);
        project.getExtensions().create(DependencyReplacement.class, "dependencyReplacements", DependencyReplacementsExtension.class, project);
        project.getExtensions().create(AccessTransformers.class, "accessTransformers", AccessTransformersExtension.class, project);
        project.getExtensions().create(Obfuscation.class, "obfuscation", ObfuscationExtension.class, project);

        project.getExtensions().create(Minecraft.class, "minecraft", MinecraftExtension.class, project);
        project.getExtensions().create(Mappings.class, "mappings", MappingsExtension.class, project);

        OfficialNamingChannelConfigurator.getInstance().configure(project);

        project.getTasks().create("handleNamingLicense", DisplayMappingsLicenseTask.class);

        project.getRepositories().maven(e -> {
            e.setUrl(Constants.MOJANG_MAVEN);
            e.metadataSources(MavenArtifactRepository.MetadataSources::artifact);
        });

        DependencyDeobfuscator.getInstance().apply(project);

        project.afterEvaluate(this::applyAfterEvaluate);

        project.getExtensions().getByType(SourceSetContainer.class)
                .configureEach(sourceSet -> sourceSet
                        .getExtensions().create(SourceSetProjectExtension.NAME, SourceSetProjectExtension.class, project));
    }

    private void applyAfterEvaluate(final Project project) {
        GradleInternalUtils.getExtensions(project.getExtensions())
                .stream()
                .filter(CommonRuntimeExtension.class::isInstance)
                .map(extension -> (CommonRuntimeExtension<?,?,?>) extension)
                .forEach(CommonRuntimeExtension::bakeDefinitions);

        project.getExtensions().getByType(Runs.class).forEach(run -> {
            if (run instanceof RunImpl) {
                final RunImpl runImpl = (RunImpl) run;
                runImpl.getModSources().get().forEach(sourceSet -> {
                    final TaskProvider<JavaCompile> compileTaskProvider = project.getTasks().named(sourceSet.getCompileJavaTaskName(), JavaCompile.class);
                    try {
                        final CommonRuntimeDefinition<?> definition = TaskDependencyUtils.realiseTaskAndExtractRuntimeDefinition(project, compileTaskProvider);
                        definition.configureRun(runImpl);
                    } catch (MultipleDefinitionsFoundException e) {
                        throw new RuntimeException("Failed to configure run: " + run.getName() + " there are multiple runtime definitions found for the source set: " + sourceSet.getName(), e);
                    }
                });
            }
        });
    }
}
