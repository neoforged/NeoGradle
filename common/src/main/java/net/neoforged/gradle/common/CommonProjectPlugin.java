package net.neoforged.gradle.common;

import net.neoforged.gradle.common.dependency.ExtraJarDependencyManager;
import net.neoforged.gradle.common.extensions.*;
import net.neoforged.gradle.common.extensions.dependency.creation.ProjectBasedDependencyCreator;
import net.neoforged.gradle.common.extensions.dependency.replacement.DependencyReplacementsExtension;
import net.neoforged.gradle.common.extensions.repository.IvyDummyRepositoryExtension;
import net.neoforged.gradle.common.runs.ide.IdeRunIntegrationManager;
import net.neoforged.gradle.common.runs.run.RunImpl;
import net.neoforged.gradle.common.runtime.definition.CommonRuntimeDefinition;
import net.neoforged.gradle.common.runtime.extensions.CommonRuntimeExtension;
import net.neoforged.gradle.common.runtime.naming.OfficialNamingChannelConfigurator;
import net.neoforged.gradle.common.tasks.DisplayMappingsLicenseTask;
import net.neoforged.gradle.common.util.TaskDependencyUtils;
import net.neoforged.gradle.common.util.constants.RunsConstants;
import net.neoforged.gradle.common.util.exceptions.MultipleDefinitionsFoundException;
import net.neoforged.gradle.common.util.run.RunsUtil;
import net.neoforged.gradle.dsl.common.extensions.*;
import net.neoforged.gradle.dsl.common.extensions.dependency.replacement.DependencyReplacement;
import net.neoforged.gradle.dsl.common.extensions.repository.Repository;
import net.neoforged.gradle.dsl.common.runs.run.Run;
import net.neoforged.gradle.dsl.common.runs.type.RunType;
import net.neoforged.gradle.dsl.common.util.NamingConstants;
import net.neoforged.gradle.util.GradleInternalUtils;
import net.neoforged.gradle.util.UrlConstants;
import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectContainer;
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
        project.getExtensions().create("extensionManager", ExtensionManager.class, project);
        project.getExtensions().create("clientExtraJarDependencyManager", ExtraJarDependencyManager.class, project);

        final ExtensionManager extensionManager = project.getExtensions().getByType(ExtensionManager.class);

        extensionManager.registerExtension("minecraft", Minecraft.class, (p) -> p.getObjects().newInstance(MinecraftExtension.class, p));
        extensionManager.registerExtension("mappings", Mappings.class, (p) -> p.getObjects().newInstance(MappingsExtension.class, p));

        OfficialNamingChannelConfigurator.getInstance().configure(project);

        project.getTasks().register("handleNamingLicense", DisplayMappingsLicenseTask.class, task -> {
            task.getLicense().set(project.provider(() -> {
                final Mappings mappings = project.getExtensions().getByType(Mappings.class);
                if (mappings.getChannel().get().getHasAcceptedLicense().get())
                    return null;
                
                return mappings.getChannel().get().getLicenseText().get();
            }));
        });

        project.getRepositories().maven(e -> {
            e.setUrl(UrlConstants.MOJANG_MAVEN);
            e.metadataSources(MavenArtifactRepository.MetadataSources::artifact);
        });

        project.afterEvaluate(this::applyAfterEvaluate);

        project.getExtensions().getByType(SourceSetContainer.class)
                .configureEach(sourceSet -> sourceSet
                        .getExtensions().create(ProjectHolder.class, ProjectHolderExtension.NAME, ProjectHolderExtension.class, project));

        project.getExtensions().add(
                RunsConstants.Extensions.RUN_TYPES,
                project.getObjects().domainObjectContainer(RunType.class, name -> project.getObjects().newInstance(RunType.class, name))
        );
        
        project.getExtensions().add(
                RunsConstants.Extensions.RUNS,
                project.getObjects().domainObjectContainer(Run.class, name -> RunsUtil.create(project, name))
        );
        
        IdeRunIntegrationManager.getInstance().setup(project);
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

        final DependencyReplacement dependencyReplacementExtension = project.getExtensions().getByType(DependencyReplacement.class);
        if (dependencyReplacementExtension instanceof DependencyReplacementsExtension) {
            final DependencyReplacementsExtension dependencyReplacementsExtension = (DependencyReplacementsExtension) dependencyReplacementExtension;
            dependencyReplacementsExtension.onPostDefinitionBakes(project);
        }

        project.getExtensions().configure(RunsConstants.Extensions.RUNS, (Action<NamedDomainObjectContainer<Run>>) runs -> runs.forEach(run -> {
            if (run instanceof RunImpl) {
                if (run.getConfigureFromTypeWithName().get()) {
                    run.configure();
                }
                
                if (run.getConfigureFromDependencies().get()) {
                    final RunImpl runImpl = (RunImpl) run;
                    runImpl.getModSources().get().forEach(sourceSet -> {
                        final Project sourceSetProject = sourceSet.getExtensions().getByType(ProjectHolder.class).getProject();
                        final TaskProvider<JavaCompile> compileTaskProvider = sourceSetProject.getTasks().named(sourceSet.getCompileJavaTaskName(), JavaCompile.class);
                        try {
                            final CommonRuntimeDefinition<?> definition = TaskDependencyUtils.realiseTaskAndExtractRuntimeDefinition(sourceSetProject, compileTaskProvider);
                            definition.configureRun(runImpl);
                        } catch (MultipleDefinitionsFoundException e) {
                            throw new RuntimeException("Failed to configure run: " + run.getName() + " there are multiple runtime definitions found for the source set: " + sourceSet.getName(), e);
                        }
                    });
                }
            }
        }));
        
        IdeRunIntegrationManager.getInstance().apply(project);
    }
}
