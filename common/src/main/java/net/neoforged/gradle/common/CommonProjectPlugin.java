package net.neoforged.gradle.common;

import net.neoforged.gradle.common.caching.CentralCacheService;
import net.neoforged.gradle.common.dependency.ExtraJarDependencyManager;
import net.neoforged.gradle.common.extensions.AccessTransformersExtension;
import net.neoforged.gradle.common.extensions.ArtifactDownloaderExtension;
import net.neoforged.gradle.common.extensions.ConfigurationDataExtension;
import net.neoforged.gradle.common.extensions.ExtensionManager;
import net.neoforged.gradle.common.extensions.IdeManagementExtension;
import net.neoforged.gradle.common.extensions.MappingsExtension;
import net.neoforged.gradle.common.extensions.MinecraftArtifactCacheExtension;
import net.neoforged.gradle.common.extensions.MinecraftExtension;
import net.neoforged.gradle.common.extensions.ProjectEvaluationExtension;
import net.neoforged.gradle.common.extensions.ProjectHolderExtension;
import net.neoforged.gradle.common.extensions.dependency.creation.ProjectBasedDependencyCreator;
import net.neoforged.gradle.common.extensions.dependency.replacement.DependencyReplacementsExtension;
import net.neoforged.gradle.common.extensions.repository.IvyDummyRepositoryExtension;
import net.neoforged.gradle.common.extensions.subsystems.SubsystemsExtension;
import net.neoforged.gradle.common.runs.ide.IdeRunIntegrationManager;
import net.neoforged.gradle.common.runs.run.RunImpl;
import net.neoforged.gradle.common.runtime.definition.CommonRuntimeDefinition;
import net.neoforged.gradle.common.runtime.extensions.RuntimesExtension;
import net.neoforged.gradle.common.runtime.naming.OfficialNamingChannelConfigurator;
import net.neoforged.gradle.common.tasks.DisplayMappingsLicenseTask;
import net.neoforged.gradle.common.util.DelegatingDomainObjectContainer;
import net.neoforged.gradle.common.util.ProjectUtils;
import net.neoforged.gradle.common.util.TaskDependencyUtils;
import net.neoforged.gradle.common.util.constants.RunsConstants;
import net.neoforged.gradle.common.util.exceptions.MultipleDefinitionsFoundException;
import net.neoforged.gradle.common.util.run.RunsUtil;
import net.neoforged.gradle.dsl.common.extensions.AccessTransformers;
import net.neoforged.gradle.dsl.common.extensions.ArtifactDownloader;
import net.neoforged.gradle.dsl.common.extensions.ConfigurationData;
import net.neoforged.gradle.dsl.common.extensions.Mappings;
import net.neoforged.gradle.dsl.common.extensions.Minecraft;
import net.neoforged.gradle.dsl.common.extensions.MinecraftArtifactCache;
import net.neoforged.gradle.dsl.common.extensions.ProjectHolder;
import net.neoforged.gradle.dsl.common.extensions.RunnableSourceSet;
import net.neoforged.gradle.dsl.common.extensions.dependency.replacement.DependencyReplacement;
import net.neoforged.gradle.dsl.common.extensions.repository.Repository;
import net.neoforged.gradle.dsl.common.extensions.subsystems.Conventions;
import net.neoforged.gradle.dsl.common.extensions.subsystems.Subsystems;
import net.neoforged.gradle.dsl.common.extensions.subsystems.conventions.Configurations;
import net.neoforged.gradle.dsl.common.extensions.subsystems.conventions.IDE;
import net.neoforged.gradle.dsl.common.extensions.subsystems.conventions.Runs;
import net.neoforged.gradle.dsl.common.extensions.subsystems.conventions.SourceSets;
import net.neoforged.gradle.dsl.common.extensions.subsystems.conventions.ide.IDEA;
import net.neoforged.gradle.dsl.common.runs.run.Run;
import net.neoforged.gradle.dsl.common.runs.type.RunType;
import net.neoforged.gradle.dsl.common.util.ConfigurationUtils;
import net.neoforged.gradle.dsl.common.util.NamingConstants;
import net.neoforged.gradle.util.UrlConstants;
import org.gradle.api.*;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;
import org.gradle.api.attributes.AttributeContainer;
import org.gradle.api.attributes.Category;
import org.gradle.api.component.AdhocComponentWithVariants;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.Delete;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.plugins.ide.eclipse.EclipsePlugin;
import org.gradle.plugins.ide.idea.IdeaPlugin;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.gradle.ext.IdeaExtPlugin;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class CommonProjectPlugin implements Plugin<Project> {

    public static final String ASSETS_SERVICE = "ng_assets";
    public static final String LIBRARIES_SERVICE = "ng_libraries";
    public static final String ACCESS_TRANSFORMER_ELEMENTS_CONFIGURATION = "accessTransformerElements";
    public static final String ACCESS_TRANSFORMER_API_CONFIGURATION = "accessTransformerApi";
    public static final String ACCESS_TRANSFORMER_CONFIGURATION = "accessTransformer";
    static final String ACCESS_TRANSFORMER_CATEGORY = "accesstransformer";

    @Override
    public void apply(Project project) {
        //Apply the evaluation extension to monitor immediate execution of indirect tasks when evaluation already happened.
        project.getExtensions().create(NamingConstants.Extension.EVALUATION, ProjectEvaluationExtension.class, project);

        project.getPluginManager().apply(JavaPlugin.class);

        //Register the services
        CentralCacheService.register(project, ASSETS_SERVICE);
        CentralCacheService.register(project, LIBRARIES_SERVICE);

        // Apply both the idea and eclipse IDE plugins
        project.getPluginManager().apply(IdeaPlugin.class);
        project.getRootProject().getPluginManager().apply(IdeaExtPlugin.class);
        project.getPluginManager().apply(IdeaExtPlugin.class);
        project.getPluginManager().apply(EclipsePlugin.class);

        project.getExtensions().create("allRuntimes", RuntimesExtension.class);
        project.getExtensions().create(IdeManagementExtension.class, "ideManager", IdeManagementExtension.class, project);
        project.getExtensions().create(ArtifactDownloader.class, "artifactDownloader", ArtifactDownloaderExtension.class, project);
        project.getExtensions().create(Repository.class, "ivyDummyRepository", IvyDummyRepositoryExtension.class, project);
        project.getExtensions().create(MinecraftArtifactCache.class, "minecraftArtifactCache", MinecraftArtifactCacheExtension.class, project);
        project.getExtensions().create(DependencyReplacement.class, "dependencyReplacements", DependencyReplacementsExtension.class, project, project.getObjects().newInstance(ProjectBasedDependencyCreator.class, project));
        AccessTransformers accessTransformers = project.getExtensions().create(AccessTransformers.class, "accessTransformers", AccessTransformersExtension.class, project);
        project.getExtensions().create("extensionManager", ExtensionManager.class, project);
        project.getExtensions().create("clientExtraJarDependencyManager", ExtraJarDependencyManager.class, project);
        final ConfigurationData configurationData = project.getExtensions().create(ConfigurationData.class, "configurationData", ConfigurationDataExtension.class, project);

        final ExtensionManager extensionManager = project.getExtensions().getByType(ExtensionManager.class);

        extensionManager.registerExtension("minecraft", Minecraft.class, (p) -> p.getObjects().newInstance(MinecraftExtension.class, p));
        extensionManager.registerExtension("mappings", Mappings.class, (p) -> p.getObjects().newInstance(MappingsExtension.class, p));
        extensionManager.registerExtension("subsystems", Subsystems.class, (p) -> p.getObjects().newInstance(SubsystemsExtension.class, p));

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

        project.getExtensions().getByType(SourceSetContainer.class).configureEach(sourceSet -> {
            sourceSet.getExtensions().create(ProjectHolder.class, ProjectHolderExtension.NAME, ProjectHolderExtension.class, project);
            sourceSet.getExtensions().create(RunnableSourceSet.NAME, RunnableSourceSet.class, project);
            sourceSet.getExtensions().add("runtimeDefinition", project.getObjects().property(CommonRuntimeDefinition.class));

            sourceSet.getExtensions().add("configurations", project.getObjects().domainObjectContainer(Configuration.class));
            final NamedDomainObjectCollection<Configuration> sourceSetConfigurations = new DelegatingDomainObjectContainer<Configuration>((NamedDomainObjectContainer<Configuration>) sourceSet.getExtensions().getByName("configurations")) {
                @Override
                public boolean add(@Nullable Configuration e) {
                    if (e == null)
                        return false;

                    return super.add(e);
                }
            };

            //Add the gradle default configurations
            sourceSetConfigurations.add(project.getConfigurations().findByName(sourceSet.getApiConfigurationName()));
            sourceSetConfigurations.add(project.getConfigurations().findByName(sourceSet.getApiElementsConfigurationName()));
            sourceSetConfigurations.add(project.getConfigurations().findByName(sourceSet.getImplementationConfigurationName()));
            sourceSetConfigurations.add(project.getConfigurations().findByName(sourceSet.getCompileOnlyConfigurationName()));
            sourceSetConfigurations.add(project.getConfigurations().findByName(sourceSet.getCompileOnlyApiConfigurationName()));
            sourceSetConfigurations.add(project.getConfigurations().findByName(sourceSet.getCompileClasspathConfigurationName()));
            sourceSetConfigurations.add(project.getConfigurations().findByName(sourceSet.getAnnotationProcessorConfigurationName()));
            sourceSetConfigurations.add(project.getConfigurations().findByName(sourceSet.getRuntimeOnlyConfigurationName()));
            sourceSetConfigurations.add(project.getConfigurations().findByName(sourceSet.getRuntimeClasspathConfigurationName()));
            sourceSetConfigurations.add(project.getConfigurations().findByName(sourceSet.getRuntimeElementsConfigurationName()));
            sourceSetConfigurations.add(project.getConfigurations().findByName(sourceSet.getJavadocElementsConfigurationName()));
            sourceSetConfigurations.add(project.getConfigurations().findByName(sourceSet.getSourcesElementsConfigurationName()));
        });

        project.getExtensions().add(
                RunsConstants.Extensions.RUN_TYPES,
                project.getObjects().domainObjectContainer(RunType.class, name -> project.getObjects().newInstance(RunType.class, name))
        );

        project.getExtensions().add(
                RunsConstants.Extensions.RUNS,
                project.getObjects().domainObjectContainer(Run.class, name -> RunsUtil.create(project, name))
        );

        setupAccessTransformerConfigurations(project, accessTransformers);

        IdeRunIntegrationManager.getInstance().setup(project);

        project.getTasks().named("clean", Delete.class, delete -> {
            delete.delete(configurationData.getLocation());
        });

        //Needs to be before after evaluate
        configureConventions(project);

        project.afterEvaluate(this::applyAfterEvaluate);
    }

    private void configureConventions(Project project) {
        final Conventions conventions = project.getExtensions().getByType(Subsystems.class).getConventions();
        if (!conventions.getIsEnabled().get())
            return;

        configureRunConventions(project, conventions);
        configureSourceSetConventions(project, conventions);
        configureIDEConventions(project, conventions);
    }

    private void configureSourceSetConventions(Project project, Conventions conventions) {
        final SourceSets sourceSets = conventions.getSourceSets();
        final Configurations configurations = conventions.getConfigurations();

        if (!sourceSets.getIsEnabled().get())
            return;

        if (configurations.getIsEnabled().get()) {
            project.getExtensions().getByType(SourceSetContainer.class).configureEach(sourceSet -> {
                final NamedDomainObjectCollection<Configuration> sourceSetConfigurations = (NamedDomainObjectCollection<Configuration>) sourceSet.getExtensions().getByName("configurations");

                final Configuration sourceSetLocalRuntimeConfiguration = project.getConfigurations().maybeCreate(ConfigurationUtils.getSourceSetName(sourceSet, configurations.getLocalRuntimeConfigurationPostFix().get()));
                sourceSetConfigurations.add(sourceSetLocalRuntimeConfiguration);

                final Configuration sourceSetRunRuntimeConfiguration = project.getConfigurations().maybeCreate(ConfigurationUtils.getSourceSetName(sourceSet, configurations.getRunRuntimeConfigurationPostFix().get()));
                sourceSetConfigurations.add(sourceSetRunRuntimeConfiguration);

                final Configuration sourceSetRuntimeClasspath = project.getConfigurations().maybeCreate(sourceSet.getRuntimeClasspathConfigurationName());
                sourceSetRuntimeClasspath.extendsFrom(sourceSetLocalRuntimeConfiguration);
            });
        }

        ProjectUtils.afterEvaluate(project, () -> {
            project.getExtensions().configure(RunsConstants.Extensions.RUNS, (Action<NamedDomainObjectContainer<Run>>) runs -> runs.configureEach(run -> {
                if (sourceSets.getShouldMainSourceSetBeAutomaticallyAddedToRuns().get())
                    run.getModSources().add(project.getExtensions().getByType(SourceSetContainer.class).getByName("main"));

                if (sourceSets.getShouldSourceSetsLocalRunRuntimesBeAutomaticallyAddedToRuns().get() && configurations.getIsEnabled().get())
                    run.getModSources().get().forEach(sourceSet -> {
                        final NamedDomainObjectCollection<Configuration> sourceSetConfigurations = (NamedDomainObjectCollection<Configuration>) sourceSet.getExtensions().getByName("configurations");
                        run.getDependencies().get().getRuntime().add(sourceSetConfigurations.getByName(ConfigurationUtils.getSourceSetName(sourceSet, configurations.getRunRuntimeConfigurationPostFix().get())));
                    });
            }));
        });

    }

    private void configureRunConventions(Project project, Conventions conventions) {
        final Configurations configurations = conventions.getConfigurations();
        final Runs runs = conventions.getRuns();

        if (!runs.getIsEnabled().get())
            return;

        if (runs.getShouldDefaultRunsBeCreated().get()) {
            final NamedDomainObjectContainer<RunType> runTypes = (NamedDomainObjectContainer<RunType>) project.getExtensions().getByName(RunsConstants.Extensions.RUN_TYPES);
            //Force none lazy resolve here.
            runTypes.whenObjectAdded(runType -> {
                project.getExtensions().configure(RunsConstants.Extensions.RUNS, (Action<NamedDomainObjectContainer<Run>>) runContainer -> {
                    if (runContainer.getAsMap().containsKey(runType.getName()))
                        return;

                    runContainer.create(runType.getName(), run -> {
                        run.configure(runType);
                    });
                });
            });
        }

        if (!configurations.getIsEnabled().get())
            return;

        final Configuration runRuntimeConfiguration = project.getConfigurations().maybeCreate(configurations.getRunRuntimeConfigurationName().get());

        project.getExtensions().configure(RunsConstants.Extensions.RUNS, (Action<NamedDomainObjectContainer<Run>>) runContainer -> runContainer.configureEach(run -> {
            final Configuration runSpecificRuntimeConfiguration = project.getConfigurations().maybeCreate(ConfigurationUtils.getRunName(run, configurations.getPerRunRuntimeConfigurationPostFix().get()));

            run.getDependencies().get().getRuntime().add(runRuntimeConfiguration);
            run.getDependencies().get().getRuntime().add(runSpecificRuntimeConfiguration);
        }));
    }

    private void configureIDEConventions(Project project, Conventions conventions) {
        final IDE ideConventions = conventions.getIde();
        if (!ideConventions.getIsEnabled().get())
            return;

        configureIDEAIDEConventions(project, ideConventions);
    }

    private void configureIDEAIDEConventions(Project project, IDE ideConventions) {
        final IDEA ideaConventions = ideConventions.getIdea();
        if (!ideaConventions.getIsEnabled().get())
            return;

        IdeRunIntegrationManager.getInstance().configureIdeaConventions(project, ideaConventions);
    }

    @SuppressWarnings("UnstableApiUsage")
    private void setupAccessTransformerConfigurations(Project project, AccessTransformers accessTransformersExtension) {
        Configuration accessTransformerElements = project.getConfigurations().maybeCreate(ACCESS_TRANSFORMER_ELEMENTS_CONFIGURATION);
        Configuration accessTransformerApi = project.getConfigurations().maybeCreate(ACCESS_TRANSFORMER_API_CONFIGURATION);
        Configuration accessTransformer = project.getConfigurations().maybeCreate(ACCESS_TRANSFORMER_CONFIGURATION);

        accessTransformerApi.setCanBeConsumed(false);
        accessTransformerApi.setCanBeResolved(false);

        accessTransformer.setCanBeConsumed(false);
        accessTransformer.setCanBeResolved(true);

        accessTransformerElements.setCanBeConsumed(true);
        accessTransformerElements.setCanBeResolved(false);
        accessTransformerElements.setCanBeDeclared(false);

        Action<AttributeContainer> action = attributes -> {
            attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.getObjects().named(Category.class, ACCESS_TRANSFORMER_CATEGORY));
        };

        accessTransformerElements.attributes(action);
        accessTransformer.attributes(action);

        accessTransformer.extendsFrom(accessTransformerApi);
        accessTransformerElements.extendsFrom(accessTransformerApi);

        // Now we set up the component, conditionally
        AdhocComponentWithVariants java = (AdhocComponentWithVariants) project.getComponents().getByName("java");
        Runnable enable = () -> java.addVariantsFromConfiguration(accessTransformerElements, variant -> {
        });

        accessTransformerElements.getAllDependencies().configureEach(dep -> enable.run());
        accessTransformerElements.getArtifacts().configureEach(artifact -> enable.run());

        // And add resolved ATs to the extension
        accessTransformersExtension.getFiles().from(accessTransformer);
    }

    private void applyAfterEvaluate(final Project project) {
        RuntimesExtension runtimesExtension = project.getExtensions().getByType(RuntimesExtension.class);
        runtimesExtension.bakeDefinitions();
        runtimesExtension.bakeDelegateDefinitions();

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
                run.configure();

                if (run.getConfigureFromDependencies().get()) {
                    final RunImpl runImpl = (RunImpl) run;

                    final Set<CommonRuntimeDefinition<?>> definitionSet = new HashSet<>();

                    runImpl.getModSources().get().forEach(sourceSet -> {
                        try {
                            final Optional<CommonRuntimeDefinition<?>> definition = TaskDependencyUtils.findRuntimeDefinition(project, sourceSet);
                            definition.ifPresent(definitionSet::add);
                        } catch (MultipleDefinitionsFoundException e) {
                            throw new RuntimeException("Failed to configure run: " + run.getName() + " there are multiple runtime definitions found for the source set: " + sourceSet.getName(), e);
                        }
                    });

                    definitionSet.forEach(definition -> {
                        definition.configureRun(runImpl);
                    });
                }
            }
        }));

        IdeRunIntegrationManager.getInstance().apply(project);
    }
}
