package net.neoforged.gradle.common;

import net.neoforged.gradle.common.accesstransformers.AccessTransformerPublishing;
import net.neoforged.gradle.common.conventions.ConventionConfigurator;
import net.neoforged.gradle.common.dependency.ExtraJarDependencyManager;
import net.neoforged.gradle.common.extensions.*;
import net.neoforged.gradle.common.extensions.dependency.replacement.ReplacementLogic;
import net.neoforged.gradle.common.extensions.problems.IProblemReporter;
import net.neoforged.gradle.common.extensions.problems.ProblemReportingConfigurator;
import net.neoforged.gradle.common.extensions.repository.IvyRepository;
import net.neoforged.gradle.common.extensions.sourcesets.SourceSetDependencyExtensionImpl;
import net.neoforged.gradle.common.extensions.sourcesets.SourceSetInheritanceExtensionImpl;
import net.neoforged.gradle.common.extensions.subsystems.SubsystemsExtension;
import net.neoforged.gradle.common.interfaceinjection.InterfaceInjectionPublishing;
import net.neoforged.gradle.common.rules.LaterAddedReplacedDependencyRule;
import net.neoforged.gradle.common.runs.ide.IdeRunIntegrationManager;
import net.neoforged.gradle.common.runs.run.RunImpl;
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
import net.neoforged.gradle.common.util.run.RunPreparationAttributes;
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
import net.neoforged.gradle.util.StringCapitalizationUtils;
import net.neoforged.gradle.util.UrlConstants;
import org.gradle.api.GradleScriptException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationPublications;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.problems.ProblemId;
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
        project.getExtensions().create(AccessTransformers.class, "accessTransformers", AccessTransformersExtension.class, project);
        project.getExtensions().create(InterfaceInjections.class, "interfaceInjections", InterfaceInjectionsExtension.class, project);

        project.getExtensions().create(Minecraft.class, "minecraft", MinecraftExtension.class, project);
        project.getExtensions().create(Mappings.class,"mappings", MappingsExtension.class, project);
        project.getExtensions().create(RunTypeManager.class, "runTypeManager", RunTypeManagerImpl.class, project);
        project.getExtensions().create(ExtraJarDependencyManager.class, "clientExtraJarDependencyManager", ExtraJarDependencyManager.class, project);
        project.getExtensions().create(RunManager.class, "runManager", RunManagerImpl.class, project);

        ProblemReportingConfigurator.configureProblemReporting(project, problems);

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

        project.getRepositories().mavenCentral();

        project.getRepositories().maven(e -> {
            e.setUrl(UrlConstants.MOJANG_MAVEN);
            e.metadataSources(MavenArtifactRepository.MetadataSources::mavenPom);
        });

        var mojangFreeTypeRepository = project.getRepositories().maven(e -> {
            e.setName("Mojang LWJGL Free Type Repository");
            e.setUrl(UrlConstants.MOJANG_MAVEN);
            e.metadataSources(MavenArtifactRepository.MetadataSources::mavenPom);
        });

        project.getRepositories().exclusiveContent(content -> {
            content.forRepositories(mojangFreeTypeRepository);
            content.filter(filter -> {
                filter.includeModule("org.lwjgl", "lwjgl-freetype");
            });
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

        //Configure sdk configurations
        final SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
        sourceSets.all(ConfigurationUtils::getSdkConfiguration);

        //Configure variant-aware run preparation configurations for isolated projects support.
        //Each source set exposes two consumable variants: one requiring compile, one resources-only.
        sourceSets.configureEach(sourceSet -> configureRunVariants(project, sourceSet));

        //Needs to be before after evaluate
        ConventionConfigurator.configureConventions(project);

        project.afterEvaluate(this::applyAfterEvaluate);
    }

    /**
     * Configures variant-aware run preparation configurations for a source set.
     * 
     * Creates two consumable configurations distinguished by the RUN_PREPARATION_MODE attribute:
     * - runCompileClasspath*: For Gradle-launched runs requiring full compilation
     * - runResourcesOnlyClasspath*: For IDE runs where only resources are needed
     * 
     * These configurations serve as variant markers for consumer-side selection. The actual task
     * dependencies are wired via file-based dependency tracking on sourceSet.getOutput(), which
     * automatically includes all build dependencies (compileJava, processResources, custom generators).
     */
    private void configureRunVariants(Project project, SourceSet sourceSet) {
        String capitalizedName = StringCapitalizationUtils.capitalize(sourceSet.getName());

        // Compile variant: marker configuration for runs requiring compilation
        Configuration compileVariant = project.getConfigurations().create("runCompileClasspath" + capitalizedName);
        compileVariant.setVisible(false);
        compileVariant.setCanBeConsumed(true);
        compileVariant.setCanBeResolved(false);
        compileVariant.attributes(attrs -> attrs.attribute(RunPreparationAttributes.RUN_PREPARATION_MODE,
            RunPreparationAttributes.MODE_COMPILE));

        // Resources-only variant: marker configuration for runs not requiring compilation
        Configuration resourcesOnlyVariant = project.getConfigurations().create("runResourcesOnlyClasspath" + capitalizedName);
        resourcesOnlyVariant.setVisible(false);
        resourcesOnlyVariant.setCanBeConsumed(true);
        resourcesOnlyVariant.setCanBeResolved(false);
        resourcesOnlyVariant.attributes(attrs -> attrs.attribute(RunPreparationAttributes.RUN_PREPARATION_MODE,
            RunPreparationAttributes.MODE_RESOURCES_ONLY));
    }

    private void applyAfterEvaluate(final Project project) {
        final JavaPluginExtension java = project.getExtensions().getByType(JavaPluginExtension.class);
        if (!java.getToolchain().getLanguageVersion().isPresent()) {
            final IProblemReporter reporter = project.getExtensions().getByType(IProblemReporter.class);
            throw reporter.throwing(
                spec -> {
                    spec.id("java", "toolchain.missing")
                        .section("java")
                        .details("NeoGradle requires a configured java toolchain to properly configure jar in jar and setup java tool executions. ")
                        .solution("Please configure it by setting the java toolchain version.")
                        .documentedAt("https://docs.gradle.org/current/userguide/toolchains.html#sec:consuming");
                }
            );
        }

        //We now eagerly get all runs and configure them.
        final RunManager runs = project.getExtensions().getByType(RunManager.class);
        runs.realizeAll(run -> RunsUtil.configure(
                project,
                run,
                !runs.getNames().contains(run.getName()) //Internal runs are not directly registered, so they don't show up in the name list.
        ));
        //Second loop over all internal and public runs to validate their configuration and emit warnings.
        runs.realizeAll(run -> {
            if (run instanceof RunImpl runImpl) {
                runImpl.performSdkValidation();
            }
        });
        IdeRunIntegrationManager.getInstance().apply(project);
    }
}
