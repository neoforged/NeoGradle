package net.neoforged.gradle.common;

import net.neoforged.gradle.common.accesstransformers.AccessTransformerPublishing;
import net.neoforged.gradle.common.conventions.ConventionConfigurator;
import net.neoforged.gradle.common.dependency.ExtraJarDependencyManager;
import net.neoforged.gradle.common.extensions.*;
import net.neoforged.gradle.common.extensions.dependency.replacement.ReplacementLogic;
import net.neoforged.gradle.common.extensions.repository.IvyRepository;
import net.neoforged.gradle.common.extensions.sourcesets.SourceSetDependencyExtensionImpl;
import net.neoforged.gradle.common.extensions.sourcesets.SourceSetInheritanceExtensionImpl;
import net.neoforged.gradle.common.extensions.subsystems.SubsystemsExtension;
import net.neoforged.gradle.common.interfaceinjection.InterfaceInjectionPublishing;
import net.neoforged.gradle.common.rules.LaterAddedReplacedDependencyRule;
import net.neoforged.gradle.common.runs.ide.IdeRunIntegrationManager;
import net.neoforged.gradle.common.runs.run.RunManagerImpl;
import net.neoforged.gradle.common.runs.run.RunTypeManagerImpl;
import net.neoforged.gradle.common.runs.tasks.RunsReport;
import net.neoforged.gradle.common.runs.unittest.UnitTestConfigurator;
import net.neoforged.gradle.common.runtime.definition.CommonRuntimeDefinition;
import net.neoforged.gradle.common.runtime.extensions.RuntimesExtension;
import net.neoforged.gradle.common.runtime.naming.OfficialNamingChannelConfigurator;
import net.neoforged.gradle.common.services.caching.CachedExecutionService;
import net.neoforged.gradle.common.tasks.CleanCache;
import net.neoforged.gradle.common.tasks.DisplayMappingsLicenseTask;
import net.neoforged.gradle.common.util.CommonRuntimeTaskUtils;
import net.neoforged.gradle.common.util.ConfigurationUtils;
import net.neoforged.gradle.common.util.run.RunsUtil;
import net.neoforged.gradle.dsl.common.extensions.*;
import net.neoforged.gradle.dsl.common.extensions.dependency.replacement.DependencyReplacement;
import net.neoforged.gradle.dsl.common.extensions.repository.Repository;
import net.neoforged.gradle.dsl.common.extensions.sourceset.RunnableSourceSet;
import net.neoforged.gradle.dsl.common.extensions.sourceset.SourceSetDependencyExtension;
import net.neoforged.gradle.dsl.common.extensions.sourceset.SourceSetInheritanceExtension;
import net.neoforged.gradle.dsl.common.extensions.subsystems.Subsystems;
import net.neoforged.gradle.dsl.common.runs.run.RunManager;
import net.neoforged.gradle.dsl.common.runs.type.RunTypeManager;
import net.neoforged.gradle.dsl.common.util.NamingConstants;
import net.neoforged.gradle.util.UrlConstants;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.problems.Problems;
import org.gradle.api.tasks.Delete;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.jvm.tasks.Jar;
import org.gradle.plugins.ide.eclipse.EclipsePlugin;
import org.gradle.plugins.ide.idea.IdeaPlugin;
import org.jetbrains.gradle.ext.IdeaExtPlugin;

import javax.inject.Inject;

public class CommonProjectPlugin implements Plugin<Project> {

    public static final String PROBLEM_NAMESPACE = "neoforged.gradle";
    public static final String PROBLEM_REPORTER_EXTENSION_NAME = "neogradleProblems";

    private final Problems problems;

    @Inject
    public CommonProjectPlugin(Problems problems) {
        this.problems = problems;
    }

    @Override
    public void apply(Project project) {
        //Apply the evaluation extension to monitor immediate execution of indirect tasks when evaluation already happened.
        project.getExtensions().create(NamingConstants.Extension.EVALUATION, ProjectEvaluationExtension.class, project);

        //We need the java plugin
        project.getPluginManager().apply(JavaPlugin.class);

        //Register the services
        CachedExecutionService.register(project);

        // Apply both the idea and eclipse IDE plugins
        project.getPluginManager().apply(IdeaPlugin.class);
        project.getRootProject().getPluginManager().apply(IdeaExtPlugin.class);
        project.getPluginManager().apply(IdeaExtPlugin.class);
        project.getPluginManager().apply(EclipsePlugin.class);

        project.getExtensions().create(Subsystems.class,"subsystems", SubsystemsExtension.class, project);
        project.getExtensions().create(IdeManagementExtension.class, "ideManager", IdeManagementExtension.class, project);
        project.getExtensions().create("allRuntimes", RuntimesExtension.class);
        project.getExtensions().create(Repository.class, "ivyDummyRepository", IvyRepository.class, project);
        project.getExtensions().create(MinecraftArtifactCache.class, "minecraftArtifactCache", MinecraftArtifactCacheExtension.class, project);
        project.getExtensions().create(DependencyReplacement.class, "dependencyReplacements", ReplacementLogic.class, project);
        project.getExtensions().create(NeoGradleProblemReporter.class, PROBLEM_REPORTER_EXTENSION_NAME, NeoGradleProblemReporter.class, problems.forNamespace(PROBLEM_NAMESPACE));
        project.getExtensions().create(AccessTransformers.class, "accessTransformers", AccessTransformersExtension.class, project);
        project.getExtensions().create(InterfaceInjections.class, "interfaceInjections", InterfaceInjectionsExtension.class, project);

        project.getExtensions().create(Minecraft.class, "minecraft", MinecraftExtension.class, project);
        project.getExtensions().create(Mappings.class,"mappings", MappingsExtension.class, project);
        project.getExtensions().create(RunTypeManager.class, "runTypeManager", RunTypeManagerImpl.class, project);
        project.getExtensions().create(ExtraJarDependencyManager.class, "clientExtraJarDependencyManager", ExtraJarDependencyManager.class, project);
        project.getExtensions().create(RunManager.class, "runManager", RunManagerImpl.class, project);

        final ConfigurationData configurationData = project.getExtensions().create(ConfigurationData.class, "configurationData", ConfigurationDataExtension.class, project);

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
            e.metadataSources(MavenArtifactRepository.MetadataSources::mavenPom);
        });

        project.getExtensions().getByType(SourceSetContainer.class).configureEach(sourceSet -> {
            sourceSet.getExtensions().create(ProjectHolder.class, ProjectHolderExtension.NAME, ProjectHolderExtension.class, project);
            sourceSet.getExtensions().create(RunnableSourceSet.NAME, RunnableSourceSet.class, project);

            sourceSet.getExtensions().create(SourceSetDependencyExtension.class, "depends", SourceSetDependencyExtensionImpl.class, sourceSet);
            sourceSet.getExtensions().create(SourceSetInheritanceExtension.class, "inherits", SourceSetInheritanceExtensionImpl.class, sourceSet);

            sourceSet.getExtensions().add("runtimeDefinition", project.getObjects().property(CommonRuntimeDefinition.class));
        });

        ConfigurationUtils.ensureReplacementConfigurationExists(project);

        //Setup IDE specific unit test handling.
        UnitTestConfigurator.configureIdeUnitTests(project);

        //Register a task creation rule that checks for runs.
        project.getTasks().addRule(new LaterAddedReplacedDependencyRule(project));

        //Set up publishing for access transformer elements
        AccessTransformerPublishing.setup(project);

        //Set up publishing for interface injection elements
        InterfaceInjectionPublishing.setup(project);

        //Set up the IDE run integration manager
        IdeRunIntegrationManager.getInstance().setup(project);

        //Clean the shared cache
        project.getTasks().register("cleanCache", CleanCache.class);

        //Clean the configuration data location.
        project.getTasks().named("clean", Delete.class, delete -> {
            delete.delete(configurationData.getLocation());
        });

        //Set up reporting tasks
        project.getTasks().register("runs", RunsReport.class);

        //Needs to be before after evaluate
        ConventionConfigurator.configureConventions(project);

        project.afterEvaluate(this::applyAfterEvaluate);
    }

    private void applyAfterEvaluate(final Project project) {
        //We now eagerly get all runs and configure them.
        final RunManager runs = project.getExtensions().getByType(RunManager.class);
        runs.realizeAll(run -> RunsUtil.configure(
                project,
                run,
                !runs.getNames().contains(run.getName()) //Internal runs are not directly registered, so they don't show up in the name list.
        ));
        IdeRunIntegrationManager.getInstance().apply(project);
    }
}
