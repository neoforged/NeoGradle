package net.neoforged.gradle.common;

import net.neoforged.gradle.common.dependency.ClientExtraJarDependencyManager;
import net.neoforged.gradle.common.dependency.MappingDebugChannelDependencyManager;
import net.neoforged.gradle.common.extensions.ExtensionManager;
import net.neoforged.gradle.common.extensions.ForcedDependencyDeobfuscationExtension;
import net.neoforged.gradle.common.extensions.IdeManagementExtension;
import net.neoforged.gradle.common.extensions.ProjectEvaluationExtension;
import net.neoforged.gradle.common.extensions.dependency.creation.ProjectBasedDependencyCreator;
import net.neoforged.gradle.common.deobfuscation.DependencyDeobfuscator;
import net.neoforged.gradle.common.extensions.AccessTransformersExtension;
import net.neoforged.gradle.common.extensions.ArtifactDownloaderExtension;
import net.neoforged.gradle.common.extensions.MappingsExtension;
import net.neoforged.gradle.common.extensions.MinecraftArtifactCacheExtension;
import net.neoforged.gradle.common.extensions.MinecraftExtension;
import net.neoforged.gradle.common.extensions.ProjectHolderExtension;
import net.neoforged.gradle.common.extensions.dependency.replacement.DependencyReplacementsExtension;
import net.neoforged.gradle.common.extensions.obfuscation.ObfuscationExtension;
import net.neoforged.gradle.common.extensions.repository.IvyDummyRepositoryExtension;
import net.neoforged.gradle.common.runs.ide.IdeRunIntegrationManager;
import net.neoforged.gradle.common.runs.run.RunsImpl;
import net.neoforged.gradle.common.runs.type.TypesImpl;
import net.neoforged.gradle.common.runtime.definition.CommonRuntimeDefinition;
import net.neoforged.gradle.common.runtime.extensions.CommonRuntimeExtension;
import net.neoforged.gradle.common.runtime.naming.OfficialNamingChannelConfigurator;
import net.neoforged.gradle.common.tasks.DisplayMappingsLicenseTask;
import net.neoforged.gradle.common.util.TaskDependencyUtils;
import net.neoforged.gradle.common.util.exceptions.MultipleDefinitionsFoundException;
import net.neoforged.gradle.common.util.constants.RunsConstants;
import net.neoforged.gradle.dsl.common.runs.type.Types;
import net.neoforged.gradle.dsl.common.util.NamingConstants;
import net.neoforged.gradle.dsl.common.extensions.AccessTransformers;
import net.neoforged.gradle.dsl.common.extensions.ArtifactDownloader;
import net.neoforged.gradle.dsl.common.extensions.Mappings;
import net.neoforged.gradle.dsl.common.extensions.Minecraft;
import net.neoforged.gradle.dsl.common.extensions.MinecraftArtifactCache;
import net.neoforged.gradle.dsl.common.extensions.ProjectHolder;
import net.neoforged.gradle.dsl.common.extensions.dependency.replacement.DependencyReplacement;
import net.neoforged.gradle.dsl.common.extensions.obfuscation.Obfuscation;
import net.neoforged.gradle.dsl.common.extensions.repository.Repository;
import net.neoforged.gradle.dsl.common.runs.run.Runs;
import net.neoforged.gradle.common.runs.run.RunImpl;
import net.neoforged.gradle.util.GradleInternalUtils;
import net.neoforged.gradle.util.UrlConstants;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.plugins.ide.eclipse.EclipsePlugin;
import org.gradle.plugins.ide.idea.IdeaPlugin;
import org.jetbrains.gradle.ext.IdeaExtPlugin;

import java.util.List;
import java.util.stream.Collectors;

public class CommonProjectPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        //Apply the evaluation extension to monitor immediate execution of indirect tasks when evaluation already happened.
        project.getExtensions().create(NamingConstants.Extension.EVALUATION, ProjectEvaluationExtension.class, project);

        project.getPluginManager().apply(JavaPlugin.class);

        // Apply both the idea and eclipse IDE plugins
        project.getPluginManager().apply(IdeaPlugin.class);
        project.getPluginManager().apply(IdeaExtPlugin.class);
        project.getPluginManager().apply(EclipsePlugin.class);

        project.getExtensions().create(IdeManagementExtension.class, "ideManager", IdeManagementExtension.class, project);
        project.getExtensions().create(ArtifactDownloader.class, "artifactDownloader", ArtifactDownloaderExtension.class, project);
        project.getExtensions().create(Repository.class, "ivyDummyRepository", IvyDummyRepositoryExtension.class, project);
        project.getExtensions().create(MinecraftArtifactCache.class, "minecraftArtifactCache", MinecraftArtifactCacheExtension.class, project);
        project.getExtensions().create(DependencyReplacement.class, "dependencyReplacements", DependencyReplacementsExtension.class, project, project.getObjects().newInstance(ProjectBasedDependencyCreator.class, project));
        project.getExtensions().create(AccessTransformers.class, "accessTransformers", AccessTransformersExtension.class, project);
        project.getExtensions().create(Obfuscation.class, "obfuscation", ObfuscationExtension.class, project);
        project.getExtensions().create("extensionManager", ExtensionManager.class, project);
        project.getExtensions().create("forcedDeobfuscation", ForcedDependencyDeobfuscationExtension.class);
        project.getExtensions().create("dependencyDeobfuscation", DependencyDeobfuscator.class, project);
        project.getExtensions().create("clientExtraJarDependencyManager", ClientExtraJarDependencyManager.class, project);

        final ExtensionManager extensionManager = project.getExtensions().getByType(ExtensionManager.class);

        extensionManager.registerExtension("minecraft", Minecraft.class, (p) -> p.getObjects().newInstance(MinecraftExtension.class, p));
        extensionManager.registerExtension("mappings", Mappings.class, (p) -> p.getObjects().newInstance(MappingsExtension.class, p));

        project.getExtensions().create("mappingDebugChannelDependencyManager", MappingDebugChannelDependencyManager.class, project);

        OfficialNamingChannelConfigurator.getInstance().configure(project);

        project.getTasks().create("handleNamingLicense", DisplayMappingsLicenseTask.class);

        project.getRepositories().maven(e -> {
            e.setUrl(UrlConstants.MOJANG_MAVEN);
            e.metadataSources(MavenArtifactRepository.MetadataSources::artifact);
        });

        project.afterEvaluate(this::applyAfterEvaluate);

        project.getExtensions().getByType(SourceSetContainer.class)
                .configureEach(sourceSet -> sourceSet
                        .getExtensions().create(ProjectHolder.class, ProjectHolderExtension.NAME, ProjectHolderExtension.class, project));

        project.getExtensions().add(
                Types.class,
                RunsConstants.Extensions.RUN_TYPES,
                project.getObjects().newInstance(TypesImpl.class, project)
        );
        project.getExtensions().add(
                Runs.class,
                RunsConstants.Extensions.RUNS,
                project.getObjects().newInstance(RunsImpl.class, project)
        );

        project.afterEvaluate(p -> {
            final Types types = p.getExtensions().getByType(Types.class);

            p.getExtensions().getByType(Runs.class)
                    .matching(run -> run instanceof RunImpl)
                    .forEach(run -> {
                        final RunImpl impl = (RunImpl) run;
                        types.matching(type -> type.getName().equals(run.getName())).forEach(impl::configureInternally);
                    });
        });

        IdeRunIntegrationManager.getInstance().apply(project);
    }

    private void applyAfterEvaluate(final Project project) {
        final List<CommonRuntimeExtension<?,?,?>> runtimeExtensions = GradleInternalUtils.getExtensions(project.getExtensions())
                .stream()
                .filter(CommonRuntimeExtension.class::isInstance)
                .map(extension -> (CommonRuntimeExtension<?,?,?>) extension)
                .collect(Collectors.toList());

        runtimeExtensions
                .forEach(CommonRuntimeExtension::bakeDefinitions);

        runtimeExtensions
                .forEach(CommonRuntimeExtension::bakeDelegateDefinitions);

        final Repository<?> repositoryExtension = project.getExtensions().getByType(Repository.class);
        if (repositoryExtension instanceof IvyDummyRepositoryExtension) {
            final IvyDummyRepositoryExtension ivyDummyRepositoryExtension = (IvyDummyRepositoryExtension) repositoryExtension;
            ivyDummyRepositoryExtension.onPostDefinitionBake(project);
        }

        project.getExtensions().getByType(Runs.class).forEach(run -> {
            if (run instanceof RunImpl) {
                if (run.getConfigureFromTypeWithName().get()) {
                    run.configure();
                }

                if (run.getConfigureFromDependencies().get()) {
                    final RunImpl runImpl = (RunImpl) run;
                    runImpl.getModSources().get().forEach(sourceSet -> {
                        try {
                            final CommonRuntimeDefinition<?> definition = TaskDependencyUtils.extractRuntimeDefinition(project, sourceSet);
                            definition.configureRun(runImpl);
                        } catch (MultipleDefinitionsFoundException e) {
                            throw new RuntimeException("Failed to configure run: " + run.getName() + " there are multiple runtime definitions found for the source set: " + sourceSet.getName(), e);
                        }
                    });
                }
            }
        });

        final DependencyReplacement dependencyReplacementExtension = project.getExtensions().getByType(DependencyReplacement.class);
        if (dependencyReplacementExtension instanceof DependencyReplacementsExtension) {
            final DependencyReplacementsExtension dependencyReplacementsExtension = (DependencyReplacementsExtension) dependencyReplacementExtension;
            dependencyReplacementsExtension.onPostDefinitionBakes(project);
        }
    }
}
