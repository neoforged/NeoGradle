package net.neoforged.gradle.common.extensions.dependency.replacement;

import com.google.common.collect.*;
import net.minecraftforge.gdi.ConfigurableDSLElement;
import net.neoforged.gradle.common.extensions.IdeManagementExtension;
import net.neoforged.gradle.common.extensions.dependency.creation.DependencyCreator;
import net.neoforged.gradle.common.tasks.ArtifactFromOutput;
import net.neoforged.gradle.dsl.common.extensions.dependency.replacement.DependencyReplacement;
import net.neoforged.gradle.dsl.common.extensions.dependency.replacement.DependencyReplacementHandler;
import net.neoforged.gradle.dsl.common.extensions.dependency.replacement.DependencyReplacementResult;
import net.neoforged.gradle.dsl.common.extensions.repository.Repository;
import net.neoforged.gradle.dsl.common.extensions.repository.RepositoryEntry;
import net.neoforged.gradle.dsl.common.extensions.repository.RepositoryReference;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import net.neoforged.gradle.dsl.common.util.Artifact;
import net.neoforged.gradle.dsl.common.util.CommonRuntimeUtils;
import net.neoforged.gradle.dsl.common.util.ConfigurationUtils;
import net.neoforged.gradle.dsl.common.util.ModuleReference;
import net.neoforged.gradle.util.TransformerUtils;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.file.Directory;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import javax.inject.Inject;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Defines the implementation of the @{link DependencyReplacement} extension.
 * <p>
 * Uses the configuration system to handle dependency replacement.
 */
public abstract class DependencyReplacementsExtension implements ConfigurableDSLElement<DependencyReplacement>, DependencyReplacement {

    private final Project project;
    private final DependencyCreator dependencyCreator;
    private final Set<ModuleReference> configuredReferences = Sets.newHashSet();
    private final Set<ModuleReference> configuredGradleTasks = Sets.newHashSet();
    private final Set<ModuleReference> configuredIdeTasks = Sets.newHashSet();
    private final Table<Dependency, Configuration, Optional<DependencyReplacementResult>> dependencyReplacementInformation = HashBasedTable.create();
    private final NamedDomainObjectContainer<DependencyReplacementHandler> dependencyReplacementHandlers;
    private boolean hasBeenBaked = false;
    private final Set<Consumer<Project>> afterDefinitionBakeCallbacks = Sets.newHashSet();

    @Inject
    public DependencyReplacementsExtension(Project project, DependencyCreator dependencyCreator) {
        this.project = project;
        this.dependencyCreator = dependencyCreator;

        //Wire up a replacement handler to each configuration for when a dependency is added.
        this.project.getConfigurations().configureEach(this::handleConfiguration);
        //Collection holder of all custom dependency replacement handlers.
        this.dependencyReplacementHandlers = this.project.getObjects().domainObjectContainer(DependencyReplacementHandler.class, name -> this.project.getObjects().newInstance(DependencyReplacementHandlerImpl.class, this.project, name));
    }

    @Override
    public void handleConfiguration(Configuration configuration) {
        configuration.getDependencies().whenObjectAdded(dependency -> {
            //We only support module based dependencies.
            if (dependency instanceof ModuleDependency) {
                final ModuleDependency moduleDependency = (ModuleDependency) dependency;
                //Try replacing the dependency.
                handleDependency(configuration, moduleDependency);
            }
        });
    }

    @Override
    public Project getProject() {
        return project;
    }

    /**
     * Indicates if the dependency replacement extension has been baked.
     *
     * @return true if the extension has been baked, false otherwise.
     * @implNote In the future, this should use the planned runtime lifecycle management instead of a direct callback.
     */
    @VisibleForTesting
    boolean hasBeenBaked() {
        return hasBeenBaked;
    }

    /**
     * Invoked by the runtime system to indicate that the project has the definitions baked.
     *
     * @param project The project that has the definitions baked.
     * @implNote In the future, this should use the planned runtime lifecycle management instead of a direct callback.
     */
    public void onPostDefinitionBakes(final Project project) {
        this.hasBeenBaked = true;
        if (project.getState().getFailure() == null) {
            this.afterDefinitionBakeCallbacks.forEach(e -> e.accept(project));
        }
    }

    @Override
    @NotNull
    public NamedDomainObjectContainer<DependencyReplacementHandler> getReplacementHandlers() {
        return this.dependencyReplacementHandlers;
    }

    /**
     * Handle the dependency replacement for the given dependency.
     *
     * @param configuration The configuration that the dependency is being added to.
     * @param dependency The dependency that is being added.
     * @implNote Currently short circuits on the first replacement handler that returns a replacement, might want to change this in the future.
     */
    @VisibleForTesting
    void handleDependency(final Configuration configuration, final ModuleDependency dependency) {
        Optional<DependencyReplacementResult> candidate;
        final Repository<?> repository = project.getExtensions().getByType(Repository.class);
        if (repository.isDynamicDependency(dependency)) {
            candidate = Optional.empty();
            dependencyReplacementInformation.put(dependency, configuration, candidate);
        } else if (dependencyReplacementInformation.contains(dependency, configuration)) {
            candidate = dependencyReplacementInformation.get(dependency, configuration);
            if (candidate == null) {
                candidate = Optional.empty();
                dependencyReplacementInformation.remove(dependency, configuration);
            }
        } else {
            candidate = Optional.empty();
            for (DependencyReplacementHandler handler : getReplacementHandlers()) {
                try {
                    Optional<DependencyReplacementResult> dependencyReplacementResult = handler.getReplacer().get().get(new DependencyReplacementContext(project, configuration, dependency, null));
                    
                    if (dependencyReplacementResult.isPresent()) {
                        candidate = dependencyReplacementResult;
                        break;
                    }
                } catch (Exception exception) {
                    getProject().getLogger().info("Failed to process dependency replacement on handler: " + exception.getMessage(), exception);
                }
            }
            if (!dependencyReplacementInformation.contains(dependency, configuration)) {
                dependencyReplacementInformation.put(dependency, configuration, candidate);
            }
        }

        candidate.ifPresent(result -> handleDependencyReplacement(configuration, dependency, result, this::handleDependencyReplacementForIde, this::handleDependencyReplacementForGradle));
    }

    /**
     * All callbacks that should be invoked when baking of runtimes completes.
     *
     * @return The set of callbacks.
     */
    @VisibleForTesting
    @NotNull
    Set<Consumer<Project>> getAfterDefinitionBakeCallbacks() {
        return afterDefinitionBakeCallbacks;
    }

    /**
     * Handle the dependency replacement for the given dependency.
     *
     * @param configuration The configuration to handle the replacement in.
     * @param dependency The dependency to replace.
     * @param result The replacement result from one of the handlers.
     * @param ideReplacer The replacer to use when the IDE is importing the project.
     * @param gradleReplacer The replacer to use when the project is being built by Gradle.
     * @implNote This method is responsible for removing the dependency from the configuration and adding the dependency provider task to the configuration.
     * @implNote Currently the gradle importer is always used, the ide replacer is however only invoked when an IDE is detected.
     */
    @VisibleForTesting
    void handleDependencyReplacement(Configuration configuration, Dependency dependency, DependencyReplacementResult result, DependencyReplacer ideReplacer, DependencyReplacer gradleReplacer) {
        configuration.getDependencies().remove(dependency);

        final List<Configuration> targetConfigurations = result.getTargetConfiguration().orElseGet(() -> {
            final List<Configuration> list = new ArrayList<>();
            list.add(configuration);
            return list;
        });
        
        final IdeManagementExtension ideManagementExtension = getProject().getExtensions().getByType(IdeManagementExtension.class);
        if (ideManagementExtension.isIdeImportInProgress()) {
            ideReplacer.handle(targetConfigurations, dependency, result);
        }
        
        gradleReplacer.handle(targetConfigurations, dependency, result);
    }

    private void handleDependencyReplacementForGradle(final List<Configuration> configurations, final Dependency dependency, final DependencyReplacementResult result) {
        createDependencyReplacementResult(configurations, dependency, result, (repoBaseDir, entry) -> {
            final ModuleReference reference = entry.toModuleReference();

            final String artifactSelectionTaskName = result.getTaskNameBuilder().apply(CommonRuntimeUtils.buildTaskName("selectRawArtifact", reference));
            if (configuredGradleTasks.contains(reference))
                return new RepositoryEntryGenerationTasks(project.getTasks().named(artifactSelectionTaskName, ArtifactFromOutput.class));

            configuredGradleTasks.add(reference);

            return new RepositoryEntryGenerationTasks(project.getTasks().register(artifactSelectionTaskName, ArtifactFromOutput.class, artifactFromOutput -> {
                artifactFromOutput.setGroup("neogradle/dependencies");
                artifactFromOutput.setDescription(String.format("Selects the raw artifact from the %s dependency and puts it in the Ivy repository", dependency));

                artifactFromOutput.getInput().set(result.getRawJarTaskProvider().flatMap(WithOutput::getOutput));
                artifactFromOutput.getOutput().fileProvider(repoBaseDir.map(TransformerUtils.guard(dir -> entry.buildArtifactPath(dir.getAsFile().toPath()).toFile())));
                artifactFromOutput.dependsOn(result.getRawJarTaskProvider());
            }));
        });
    }

    private void handleDependencyReplacementForIde(final List<Configuration> configurations, final Dependency dependency, final DependencyReplacementResult result) {
        createDependencyReplacementResult(configurations, dependency, result, ((repoBaseDir, entry) -> {
            final ModuleReference reference = entry.toModuleReference();

            final String rawArtifactSelectorName = result.getTaskNameBuilder().apply(CommonRuntimeUtils.buildTaskName("selectRawArtifact", reference));
            final String sourceArtifactSelectorName = result.getTaskNameBuilder().apply(CommonRuntimeUtils.buildTaskName("selectSourceArtifact", reference));

            if (configuredIdeTasks.contains(reference)) {
                final TaskProvider<? extends WithOutput> rawProvider = project.getTasks().named(rawArtifactSelectorName, WithOutput.class);
                final TaskProvider<? extends WithOutput> sourceProvider = project.getTasks().named(sourceArtifactSelectorName, WithOutput.class);

                return new RepositoryEntryGenerationTasks(rawProvider, sourceProvider);
            }

            configuredIdeTasks.add(reference);
            final TaskProvider<? extends WithOutput> rawProvider = project.getTasks().register(rawArtifactSelectorName, ArtifactFromOutput.class, artifactFromOutput -> {
                artifactFromOutput.setGroup("neogradle/dependencies");
                artifactFromOutput.setDescription(String.format("Selects the raw artifact from the %s dependency and puts it in the Ivy repository", dependency));

                artifactFromOutput.getInput().set(result.getRawJarTaskProvider().flatMap(WithOutput::getOutput));
                artifactFromOutput.getOutput().fileProvider(repoBaseDir.map(TransformerUtils.guard(dir -> entry.buildArtifactPath(dir.getAsFile().toPath()).toFile())));
                artifactFromOutput.dependsOn(result.getRawJarTaskProvider());
            });

            final TaskProvider<? extends WithOutput> sourceProvider = project.getTasks().register(sourceArtifactSelectorName, ArtifactFromOutput.class, artifactFromOutput -> {
                artifactFromOutput.setGroup("neogradle/dependencies");
                artifactFromOutput.setDescription(String.format("Selects the source artifact from the %s dependency and puts it in the Ivy repository", dependency));

                artifactFromOutput.getInput().set(result.getSourcesJarTaskProvider().flatMap(WithOutput::getOutput));
                artifactFromOutput.getOutput().fileProvider(repoBaseDir.map(TransformerUtils.guard(dir -> entry.asSources().buildArtifactPath(dir.getAsFile().toPath()).toFile())));
                artifactFromOutput.dependsOn(result.getSourcesJarTaskProvider());
            });

            return new RepositoryEntryGenerationTasks(rawProvider, sourceProvider);
        }));
    }

    @VisibleForTesting
    void createDependencyReplacementResult(final List<Configuration> configurations, final Dependency dependency, final DependencyReplacementResult result, final TaskProviderGenerator generator) {
        if (!(dependency instanceof ExternalModuleDependency)) {
            return;
        }

        final ExternalModuleDependency externalModuleDependency = (ExternalModuleDependency) dependency;

        final Repository<?> extension = project.getExtensions().getByType(Repository.class);
        final Provider<Directory> repoBaseDir = extension.getRepositoryDirectory();
        try {
            extension.withDependency(
                    builder -> configureRepositoryReference(result, externalModuleDependency, builder),
                    reference -> processRepositoryReference(configurations, result, generator, repoBaseDir, reference),
                    builder -> configureRepositoryEntry(result, externalModuleDependency, builder),
                    entry -> processRepositoryEntry(configurations, result, generator, repoBaseDir, entry),
                    result.getProcessImmediately()
            );
        } catch (XMLStreamException | IOException e) {
            throw new RuntimeException(String.format("Failed to create the dummy dependency for: %s", dependency), e);
        }
    }

    private void processRepositoryReference(List<Configuration> configurations, DependencyReplacementResult result, TaskProviderGenerator generator, Provider<Directory> repoBaseDir, RepositoryReference entry) {
        final Dependency replacedDependency = entry.toGradle(project);
        
        configurations.forEach(configuration -> {
            final Configuration container = project.getConfigurations().maybeCreate("ng_dummy_ng_" + configuration.getName());
            container.getDependencies().add(replacedDependency);
            configuration.extendsFrom(container);
        });
        result.getOnCreateReplacedDependencyCallback().accept(replacedDependency);
    }


    private void processRepositoryEntry(List<Configuration> configurations, DependencyReplacementResult result, TaskProviderGenerator generator, Provider<Directory> repoBaseDir, RepositoryEntry<?, ?> entry) {
        final ModuleReference reference = entry.toModuleReference();
        if (configuredReferences.contains(reference))
            return;

        configuredReferences.add(reference);

        final RepositoryEntryGenerationTasks entryGenerationTasks = generator.generate(repoBaseDir, entry);
        final Dependency replacedDependency = this.dependencyCreator.from(entryGenerationTasks.getRawJarProvider());
        configurations.forEach(configuration -> {
            final Configuration container = project.getConfigurations().maybeCreate("ng_dummy_ng_" + configuration.getName());
            container.getDependencies().add(replacedDependency);
            configuration.extendsFrom(container);
        });
        result.getOnRepoWritingTaskRegisteredCallback().accept(entryGenerationTasks.getRawJarProvider());

        afterDefinitionBake(projectAfterBake -> {
            final IdeManagementExtension ideManagementExtension = getProject().getExtensions().getByType(IdeManagementExtension.class);
            if (ideManagementExtension.isIdeImportInProgress()) {
                ideManagementExtension.registerTaskToRun(entryGenerationTasks.getRawJarProvider());
                entryGenerationTasks.getSourceJarProvider().ifPresent(ideManagementExtension::registerTaskToRun);
                result.getAdditionalIdePostSyncTasks().forEach(ideManagementExtension::registerTaskToRun);
            }
        });
    }

    private void configureRepositoryReference(DependencyReplacementResult result, ExternalModuleDependency externalModuleDependency, RepositoryReference.Builder<?, ?> builder) {
        builder.from(externalModuleDependency);
        result.getReferenceConfigurator().accept(builder);
    }

    private void configureRepositoryEntry(DependencyReplacementResult result, ExternalModuleDependency externalModuleDependency, RepositoryEntry.Builder<?, ?, ?> builder) {
        builder.from(externalModuleDependency);
        result.getMetadataConfigurator().accept(builder);

        result.getAdditionalDependenciesConfiguration().getAllDependencies()
                .stream()
                .filter(ExternalModuleDependency.class::isInstance)
                .map(ExternalModuleDependency.class::cast)
                .forEach(additionalDependency -> builder.withDependency(depBuilder -> depBuilder.from(additionalDependency)));
    }

    @Override
    public void afterDefinitionBake(final Consumer<Project> callback) {
        if (this.hasBeenBaked) {
            callback.accept(this.project);
            return;
        }

        this.afterDefinitionBakeCallbacks.add(callback);
    }

    @VisibleForTesting
    public static class RepositoryEntryGenerationTasks {
        @NotNull
        private final TaskProvider<? extends WithOutput> rawJarProvider;
        @Nullable
        private final TaskProvider<? extends WithOutput> sourceJarProvider;

        public RepositoryEntryGenerationTasks(@NotNull TaskProvider<? extends WithOutput> rawJarProvider) {
            this.rawJarProvider = rawJarProvider;
            this.sourceJarProvider = null;
        }

        public RepositoryEntryGenerationTasks(@NotNull TaskProvider<? extends WithOutput> rawJarProvider, @Nullable TaskProvider<? extends WithOutput> sourceJarProvider) {
            this.rawJarProvider = rawJarProvider;
            this.sourceJarProvider = sourceJarProvider;
        }

        public TaskProvider<? extends WithOutput> getRawJarProvider() {
            return rawJarProvider;
        }

        public Optional<TaskProvider<? extends WithOutput>> getSourceJarProvider() {
            return Optional.ofNullable(sourceJarProvider);
        }
    }

    @VisibleForTesting
    @FunctionalInterface
    interface TaskProviderGenerator {

        RepositoryEntryGenerationTasks generate(final Provider<Directory> repoBaseDir, final RepositoryEntry<?,?> entry);
    }


    @VisibleForTesting
    @FunctionalInterface
    interface DependencyReplacer {
        void handle(final List<Configuration> configurations, final Dependency dependency, final DependencyReplacementResult result);
    }
}
