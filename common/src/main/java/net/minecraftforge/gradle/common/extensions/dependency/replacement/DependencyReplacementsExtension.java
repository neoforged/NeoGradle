package net.minecraftforge.gradle.common.extensions.dependency.replacement;

import com.google.common.collect.Sets;
import net.minecraftforge.gdi.ConfigurableDSLElement;
import net.minecraftforge.gradle.common.extensions.dependency.creation.DependencyCreator;
import net.minecraftforge.gradle.util.TransformerUtils;
import net.minecraftforge.gradle.common.extensions.IdeManagementExtension;
import net.minecraftforge.gradle.common.tasks.ArtifactFromOutput;
import net.minecraftforge.gradle.common.tasks.DependencyGenerationTask;
import net.minecraftforge.gradle.common.tasks.RawAndSourceCombiner;
import net.minecraftforge.gradle.dsl.common.extensions.dependency.replacement.DependencyReplacement;
import net.minecraftforge.gradle.dsl.common.extensions.dependency.replacement.DependencyReplacementHandler;
import net.minecraftforge.gradle.dsl.common.extensions.dependency.replacement.DependencyReplacementResult;
import net.minecraftforge.gradle.dsl.common.extensions.repository.Repository;
import net.minecraftforge.gradle.dsl.common.extensions.repository.RepositoryEntry;
import net.minecraftforge.gradle.dsl.common.tasks.WithOutput;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.NamedDomainObjectFactory;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.file.Directory;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;

import javax.inject.Inject;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
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
    private final TaskProvider<? extends DependencyGenerationTask> dependencyGenerator;
    private final Set<Configuration> configuredConfigurations = Sets.newHashSet();
    private final NamedDomainObjectContainer<DependencyReplacementHandler> dependencyReplacementHandlers;
    private boolean registeredTaskToIde;
    private boolean hasBeenBaked = false;
    private final Set<Consumer<Project>> afterDefinitionBakeCallbacks = Sets.newHashSet();

    @SuppressWarnings("unchecked")
    @Inject
    public DependencyReplacementsExtension(Project project, DependencyCreator dependencyCreator) {
        this.project = project;
        this.dependencyCreator = dependencyCreator;

        //Wire up a replacement handler to each configuration for when a dependency is added.
        this.project.getConfigurations().configureEach(configuration -> configuration.getDependencies().whenObjectAdded(dependency -> {
            //We only support module based dependencies.
            if (dependency instanceof ModuleDependency) {
                final ModuleDependency moduleDependency = (ModuleDependency) dependency;
                //Try replacing the dependency.
                handleDependency(configuration, moduleDependency);
            }
        }));

        //This is the root task that gets wired up to every configuration which has its dependency replaced.
        this.dependencyGenerator = this.project.getTasks().register("generateDependencies", DependencyGenerationTask.class);

        //Collection holder of all custom dependency replacement handlers.
        this.dependencyReplacementHandlers = this.project.getObjects().domainObjectContainer(DependencyReplacementHandler.class, name -> this.project.getObjects().newInstance(DependencyReplacementHandlerImpl.class, this.project, name));
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
    @SuppressWarnings("OptionalGetWithoutIsPresent") //Two lines above the get....
    void handleDependency(final Configuration configuration, final ModuleDependency dependency) {
        Set<DependencyReplacementHandler> replacementHandlers = getReplacementHandlers();
        replacementHandlers.stream()
                .map(handler -> handler.getReplacer().get() .get(new DependencyReplacementContext(project, configuration, dependency)))
                .filter(Optional::isPresent)
                .findFirst()
                .map(Optional::get)
                .ifPresent(result -> handleDependencyReplacement(configuration, dependency, result, this::handleDependencyReplacementForIde, this::handleDependencyReplacementForGradle));
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
     * A set of all configurations which have at least one dependency replaced and as such are configured to run the relevant
     * generation tasks to provide the dependencies.
     *
     * @return The set of configurations.
     */
    @VisibleForTesting
    @NotNull
    Set<Configuration> getConfiguredConfigurations() {
        return configuredConfigurations;
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

        registerDependencyProviderTaskIfNecessaryTo(configuration);

        final IdeManagementExtension ideManagementExtension = getProject().getExtensions().getByType(IdeManagementExtension.class);

        if (ideManagementExtension.isIdeImportInProgress()) {
            ideReplacer.handle(configuration, dependency, result);
        }

        gradleReplacer.handle(configuration, dependency, result);
    }

    /**
     * Registers the dependency provider task to the given configuration if it has not already been registered.
     * 
     * @param configuration The configuration to register the task to.
     */
    @VisibleForTesting
    void registerDependencyProviderTaskIfNecessaryTo(Configuration configuration) {
        if (!this.configuredConfigurations.contains(configuration)) {
            this.configuredConfigurations.add(configuration);
            configuration.getDependencies().add(this.dependencyCreator.from(this.dependencyGenerator.get()));
        }

        if (!registeredTaskToIde) {
            final IdeManagementExtension ideManagementExtension = getProject().getExtensions().getByType(IdeManagementExtension.class);
            ideManagementExtension.registerTaskToRun(dependencyGenerator);
            registeredTaskToIde = true;
        }
    }

    private void handleDependencyReplacementForGradle(final Configuration configuration, final Dependency dependency, final DependencyReplacementResult result) {
        createDependencyReplacementResult(configuration, dependency, result, (repoBaseDir, entry) -> {
            final String artifactSelectionTaskName = result.getTaskNameBuilder().apply("selectRawArtifact");
            return project.getTasks().register(artifactSelectionTaskName, ArtifactFromOutput.class, artifactFromOutput -> {
                artifactFromOutput.setGroup("forgegradle/dependencies");
                artifactFromOutput.setDescription(String.format("Selects the raw artifact from the %s dependency and puts it in the Ivy repository", dependency));

                artifactFromOutput.getInput().set(result.getRawJarTaskProvider().flatMap(WithOutput::getOutput));
                artifactFromOutput.getOutput().fileProvider(repoBaseDir.map(TransformerUtils.guard(dir -> entry.buildArtifactPath(dir.getAsFile().toPath()).toFile())));
                artifactFromOutput.dependsOn(result.getRawJarTaskProvider());
            });
        });
    }

    private void handleDependencyReplacementForIde(final Configuration configuration, final Dependency dependency, final DependencyReplacementResult result) {
        createDependencyReplacementResult(configuration, dependency, result, ((repoBaseDir, entry) -> {
            final String dependencyExporterTaskName = result.getTaskNameBuilder().apply("combined");
            return project.getTasks().register(dependencyExporterTaskName, RawAndSourceCombiner.class, rawAndSourceCombiner -> {
                rawAndSourceCombiner.setGroup("forgegradle/dependencies");
                rawAndSourceCombiner.setDescription(String.format("Combines the raw and sources jars into a single task execution tree for: %s", dependency.toString()));

                rawAndSourceCombiner.getRawJarInput().set(result.getRawJarTaskProvider().flatMap(WithOutput::getOutput));
                rawAndSourceCombiner.getSourceJarInput().set(result.getSourcesJarTaskProvider().flatMap(WithOutput::getOutput));

                rawAndSourceCombiner.getRawJarOutput().fileProvider(repoBaseDir.map(TransformerUtils.guard(dir -> entry.buildArtifactPath(dir.getAsFile().toPath()).toFile())));
                rawAndSourceCombiner.getSourceJarOutput().fileProvider(repoBaseDir.map(TransformerUtils.guard(dir -> entry.asSources().buildArtifactPath(dir.getAsFile().toPath()).toFile())));
            });
        }));
    }

    @VisibleForTesting
    void createDependencyReplacementResult(final Configuration configuration, final Dependency dependency, final DependencyReplacementResult result, final TaskProviderGenerator generator) {
        if (!(dependency instanceof ExternalModuleDependency)) {
            return;
        }

        final ExternalModuleDependency externalModuleDependency = (ExternalModuleDependency) dependency;

        final Repository<?,?,?,?,?> extension = project.getExtensions().getByType(Repository.class);
        final Provider<Directory> repoBaseDir = extension.getRepositoryDirectory();
        try {
            extension.withDependency(
                    builder -> configureRepositoryEntry(result, externalModuleDependency, builder),
                    entry -> processRepositoryEntry(configuration, result, generator, repoBaseDir, entry)
            );
        } catch (XMLStreamException | IOException e) {
            throw new RuntimeException(String.format("Failed to create the dummy dependency for: %s", dependency), e);
        }
    }

    private void processRepositoryEntry(Configuration configuration, DependencyReplacementResult result, TaskProviderGenerator generator, Provider<Directory> repoBaseDir, RepositoryEntry<?, ?> entry) {
        final TaskProvider<? extends Task> rawAndSourceCombinerTask = generator.generate(repoBaseDir, entry);

        final Dependency replacedDependency = entry.toGradle(project);
        configuration.getDependencies().add(replacedDependency);
        result.getOnCreateReplacedDependencyCallback().accept(replacedDependency);

        afterDefinitionBake(projectAfterBake -> {
            this.dependencyGenerator.configure(task -> {
                task.dependsOn(rawAndSourceCombinerTask);
                //noinspection Convert2MethodRef -> Gradle Groovy shit fails.
                result.getAdditionalIdePostSyncTasks().forEach(postSyncTask -> task.dependsOn(postSyncTask));
            });
        });
    }

    private void configureRepositoryEntry(DependencyReplacementResult result, ExternalModuleDependency externalModuleDependency, RepositoryEntry.Builder<?, ?, ?> builder) {
        builder.from(externalModuleDependency);
        result.getDependencyMetadataConfigurator().accept(builder);

        result.getAdditionalDependenciesConfiguration().getDependencies()
                .stream()
                .filter(ExternalModuleDependency.class::isInstance)
                .map(ExternalModuleDependency.class::cast)
                .forEach(additionalDependency -> builder.withDependency(depBuilder -> depBuilder.from(additionalDependency)));
    }

    @VisibleForTesting
    void afterDefinitionBake(final Consumer<Project> callback) {
        if (this.hasBeenBaked) {
            callback.accept(this.project);
            return;
        }

        this.afterDefinitionBakeCallbacks.add(callback);
    }

    @VisibleForTesting
    @FunctionalInterface
    interface TaskProviderGenerator {

        TaskProvider<? extends Task> generate(final Provider<Directory> repoBaseDir, final RepositoryEntry<?,?> entry);
    }

    @VisibleForTesting
    @FunctionalInterface
    interface DependencyReplacer {
        void handle(final Configuration configuration, final Dependency dependency, final DependencyReplacementResult result);
    }
}
