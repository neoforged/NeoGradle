package net.neoforged.gradle.common.extensions.dependency.replacement;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import net.minecraftforge.gdi.ConfigurableDSLElement;
import net.neoforged.gradle.common.extensions.IdeManagementExtension;
import net.neoforged.gradle.common.tasks.ArtifactFromOutput;
import net.neoforged.gradle.dsl.common.extensions.dependency.replacement.DependencyReplacement;
import net.neoforged.gradle.dsl.common.extensions.dependency.replacement.DependencyReplacementHandler;
import net.neoforged.gradle.dsl.common.extensions.dependency.replacement.ReplacementAware;
import net.neoforged.gradle.dsl.common.extensions.dependency.replacement.ReplacementResult;
import net.neoforged.gradle.dsl.common.extensions.repository.Entry;
import net.neoforged.gradle.dsl.common.extensions.repository.Repository;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import net.neoforged.gradle.dsl.common.util.CommonRuntimeUtils;
import net.neoforged.gradle.dsl.common.util.ConfigurationUtils;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.api.artifacts.*;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import javax.inject.Inject;
import java.util.*;

/**
 * Defines the implementation of the @{link DependencyReplacement} extension.
 * <p>
 * Uses the configuration system to handle dependency replacement.
 */
public abstract class ReplacementLogic implements ConfigurableDSLElement<DependencyReplacement>, DependencyReplacement {

    private final Project project;

    private final Table<Dependency, Configuration, Optional<ReplacementResult>> dependencyReplacementInformation = HashBasedTable.create();
    private final Table<Dependency, ReplacementResult, Entry> repositoryEntries = HashBasedTable.create();
    private final Table<Dependency, Configuration, Dependency> originalDependencyLookup = HashBasedTable.create();
    private final NamedDomainObjectContainer<DependencyReplacementHandler> dependencyReplacementHandlers;

    private final List<DependencyReplacedCallback> whenDependencyReplaced = new ArrayList<>();

    @Inject
    public ReplacementLogic(Project project) {
        this.project = project;

        //Wire up a replacement handler to each configuration for when a dependency is added.
        this.project.getConfigurations().configureEach(this::handleConfiguration);
        //Collection holder of all custom dependency replacement handlers.
        this.dependencyReplacementHandlers = this.project.getObjects().domainObjectContainer(DependencyReplacementHandler.class, name -> this.project.getObjects().newInstance(Handler.class, this.project, name));
    }

    @Override
    public void whenDependencyReplaced(DependencyReplacedCallback dependencyAction) {
        this.whenDependencyReplaced.add(dependencyAction);

        for (Table.Cell<Dependency, Configuration, Dependency> dependencyConfigurationDependencyCell : this.originalDependencyLookup.cellSet()) {
            dependencyAction.apply(dependencyConfigurationDependencyCell.getRowKey(), dependencyConfigurationDependencyCell.getColumnKey(), dependencyConfigurationDependencyCell.getValue());
        }
    }

    @Override
    public void handleConfiguration(Configuration configuration) {
        //TODO: Figure out if there is any way to do this lazily.
        //TODO: Configure each runs in an immutable context, so we can't add a listener to the dependencies.
        configuration.getDependencies().configureEach(dependency -> {
            //We need to check if our configuration is unhandled, we can only do this here and not in the register because of way we register unhandled configurations after their creation:
            if (ConfigurationUtils.isUnhandledConfiguration(configuration)) {
                //We don't handle this configuration.
                return;
            }

            //We only support module based dependencies.
            if (dependency instanceof ModuleDependency moduleDependency) {
                //Try replacing the dependency.
                onDependencyAdded(configuration, moduleDependency);
            }
        });

        configuration.withDependencies(dependencyContainer -> {
            //We need to check if our configuration is unhandled, we can only do this here and not in the register because of way we register unhandled configurations after their creation:
            if (ConfigurationUtils.isUnhandledConfiguration(configuration)) {
                //We don't handle this configuration.
                return;
            }

            final Set<Dependency> currentDependencies = new HashSet<>(dependencyContainer);
            currentDependencies.forEach(dependency -> {
                //We only support module based dependencies.
                if (dependency instanceof ModuleDependency moduleDependency) {
                    //Try replacing the dependency.
                    handleDependency(configuration, dependencyContainer, moduleDependency);
                }
            });
        });
    }

    @Override
    public Project getProject() {
        return project;
    }

    @Override
    @NotNull
    public NamedDomainObjectContainer<DependencyReplacementHandler> getReplacementHandlers() {
        return this.dependencyReplacementHandlers;
    }

    @NotNull
    @Override
    public Dependency optionallyConvertBackToOriginal(@NotNull final Dependency dependency, Configuration configuration) {
        final Dependency originalDependency = originalDependencyLookup.get(dependency, configuration);
        if (originalDependency == null && !configuration.getExtendsFrom().isEmpty()) {
            //Check if we have a parent configuration that might have the original dependency.
            for (Configuration parentConfiguration : configuration.getExtendsFrom()) {
                return optionallyConvertBackToOriginal(dependency, parentConfiguration);
            }
        } else if (originalDependency != null) {
            return originalDependency;
        }

        return dependency;
    }

    /**
     * Determine the replacement result for the given dependency.
     *
     * @param configuration The configuration that the dependency is being added to.
     * @param dependency The dependency that is being added.
     * @return The replacement result for the given dependency.
     */
    private Optional<ReplacementResult> determineReplacementResult(final Configuration configuration, final ModuleDependency dependency) {
        Optional<ReplacementResult> candidate;
        final Repository repository = project.getExtensions().getByType(Repository.class);
        //First check if we have an already dynamic dependency.
        if (repository.isDynamicDependency(dependency)) {
            //Is already dynamic, so we don't need to replace it.
            candidate = Optional.empty();
            dependencyReplacementInformation.put(dependency, configuration, candidate);
        }
        //Then check if we already replaced this before.
        else if (dependencyReplacementInformation.contains(dependency, configuration)) {
            //If so, use the cached result.
            candidate = dependencyReplacementInformation.get(dependency, configuration);
            //Validate that we have a sane state.
            if (candidate == null || candidate.isEmpty()) {
                //State is insane, invalidate the cache.
                candidate = Optional.empty();
                dependencyReplacementInformation.remove(dependency, configuration);
            }
        }
        //Okey, no cache, not a dynamic dependency, so we need to check the handlers.
        else {
            //Initialize
            candidate = Optional.empty();

            //Check each handler for a replacement.
            for (DependencyReplacementHandler handler : getReplacementHandlers()) {
                try {
                    //Ask the handler for a replacement.
                    Optional<ReplacementResult> dependencyReplacementResult = handler.getReplacer().get().get(new Context(project, configuration, dependency, null));

                    //Check if the handler returned a replacement.
                    if (dependencyReplacementResult.isPresent()) {
                        //Replacement found, skip the remaining handlers.
                        candidate = dependencyReplacementResult;
                        break;
                    }
                } catch (Exception exception) {
                    //Fail fast on exceptions.
                    throw new GradleException("Uncaught exception while processing replacement of dependency " + dependency.getGroup() + ":" + dependency.getName()
                            + " using handler " + handler + ": "  + exception.getMessage(), exception);
                }
            }

            //Cache the result.
            if (!dependencyReplacementInformation.contains(dependency, configuration)) {
                dependencyReplacementInformation.put(dependency, configuration, candidate);
            }
        }

        return candidate;
    }

    /**
     * Handle the dependency replacement for the given dependency.
     *
     * @param configuration The configuration that the dependency is being added to.
     * @param dependency The dependency that is being added.
     * @implNote Currently short circuits on the first replacement handler that returns a replacement, might want to change this in the future.
     */
    private void onDependencyAdded(final Configuration configuration, final ModuleDependency dependency) {
        //Check if we are going to replace this.
        final Optional<ReplacementResult> candidate = determineReplacementResult(configuration, dependency);

        //If so handle the prospective replacement data.
        if (candidate.isPresent()) {
            final ReplacementResult result = candidate.get();
            handleProspectiveDependencyReplacement(dependency, result);
        }
    }

    /**
     * Handle the dependency replacement for the given dependency.
     *
     * @param configuration The configuration that the dependency is being added to.
     * @param dependency The dependency that is being added.
     * @implNote Currently short circuits on the first replacement handler that returns a replacement, might want to change this in the future.
     */
    private void handleDependency(final Configuration configuration, final DependencySet dependencyContainer, final ModuleDependency dependency) {
        final Optional<ReplacementResult> candidate = determineReplacementResult(configuration, dependency);

        //Check if we have a candidate
        if(candidate.isPresent()) {
            //We have a candidate, handle the replacement.
            final ReplacementResult result = candidate.get();
            handleDependencyReplacement(configuration, dependencyContainer, dependency, result);
        }
    }

    /**
     * Method that ensure that the data needed for future replacement is stored and ready.
     * Creates tasks, and ensures that dependency data is present.
     *
     * @param dependency The dependency that is being replaced.
     * @param result The replacement result from one of the handlers.
     */
    private void handleProspectiveDependencyReplacement(final ModuleDependency dependency, final ReplacementResult result) {
        //Create a new dependency in our dummy repo
        final Entry newRepoEntry = createDummyDependency(dependency, result);

        registerTasks(dependency, result, newRepoEntry);

        if (result instanceof ReplacementAware replacementAware) {
            replacementAware.onTargetDependencyAdded();
        }
    }

    /**
     * Handle the dependency replacement for the given dependency.
     *
     * @param configuration The configuration to handle the replacement in.
     * @param dependency The dependency to replace.
     * @param result The replacement result from one of the handlers.
     * @implNote This method is responsible for removing the dependency from the configuration and adding the dependency provider task to the configuration.
     * @implNote Currently the gradle importer is always used, the ide replacer is however only invoked when an IDE is detected.
     */
    private void handleDependencyReplacement(Configuration configuration, DependencySet configuredSet, Dependency dependency, ReplacementResult result) {
        //Remove the initial dependency.
        configuredSet.remove(dependency);

        //Create a new dependency in our dummy repo
        final Entry newRepoEntry = createDummyDependency(dependency, result);

        //Create and register the tasks that build the replacement dependency.
        final TaskProvider<? extends WithOutput> rawTask = registerTasks(dependency, result, newRepoEntry);

        //Find the configurations that the dependency should be replaced in.
        final List<Configuration> targetConfigurations = ConfigurationUtils.findReplacementConfigurations(project, configuration);

        //For each configuration that we target we now need to add the new dependencies to.
        for (Configuration targetConfiguration : targetConfigurations) {
            try {
                //Create a dependency from the tasks that copies the raw jar to the repository.
                //The sources jar is not needed here.
                final Provider<ConfigurableFileCollection> replacedFiles = createDependencyFromTask(rawTask);

                //Add the new dependency to the target configuration.
                final DependencySet targetDependencies = targetConfiguration == configuration ?
                        configuredSet :
                        targetConfiguration.getDependencies();

                final Provider<Dependency> replacedDependencies = replacedFiles
                        .map(files -> project.getDependencies().create(files));
                final Provider<Dependency> newRepoDependency = project.provider(newRepoEntry::getDependency);

                //Add the new dependency to the target configuration.
                targetDependencies.addLater(replacedDependencies);
                targetDependencies.addLater(newRepoDependency);

                //Keep track of the original dependency, so we can convert back if needed.
                originalDependencyLookup.put(newRepoEntry.getDependency(), targetConfiguration, dependency);

                for (DependencyReplacedCallback dependencyReplacedCallback : this.whenDependencyReplaced) {
                    dependencyReplacedCallback.apply(newRepoEntry.getDependency(), targetConfiguration, dependency);
                }
            } catch (Exception exception) {
                throw new GradleException("Failed to add the replaced dependency to the configuration " + targetConfiguration.getName() + ": " + exception.getMessage(), exception);
            }
        }
    }

    /**
     * Method invoked to register the tasks needed to replace a given dependency using the given replacement result.
     *
     * @param dependency The dependency that is being replaced.
     * @param result The replacement result from one of the handlers.
     * @param newRepoEntry The new repository entry that the dependency is being replaced with.
     * @return The task that selects the raw artifact from the dependency and puts it in the Ivy repository.
     */
    private TaskProvider<? extends WithOutput> registerTasks(Dependency dependency, ReplacementResult result, Entry newRepoEntry) {
        final boolean requiresSourcesJar = result.getSourcesJar() != null;

        //Determine the task names for the tasks that copy the artifacts to the repository.
        final String rawArtifactSelectorName = CommonRuntimeUtils.buildTaskName("selectRawArtifact", newRepoEntry.getDependency());
        @Nullable
        final String sourceArtifactSelectorName = requiresSourcesJar ? CommonRuntimeUtils.buildTaskName("selectSourceArtifact", newRepoEntry.getDependency()) : null;

        //Check if we need to create new tasks.
        final boolean createsNewTasks = !project.getTasks().getNames().contains(rawArtifactSelectorName) || (requiresSourcesJar && !project.getTasks().getNames().contains(sourceArtifactSelectorName));

        //Create them, or read them from the task container.
        final TaskProvider<? extends WithOutput> rawTask = createOrLookupRawTask(dependency, result, rawArtifactSelectorName, newRepoEntry);
        final TaskProvider<? extends WithOutput> sourceTask = requiresSourcesJar ? createOrLookupSourcesTask(dependency, result, sourceArtifactSelectorName, newRepoEntry) : null;

        //If the result is replacement aware we need to notify it.
        if (result instanceof ReplacementAware replacementAware) {
            replacementAware.onTasksCreated(
                    rawTask,
                    sourceTask
            );
        }

        //When we create new tasks, we need to register them with the IDE importer.
        if (createsNewTasks) {
            final IdeManagementExtension ideManagementExtension = getProject().getExtensions().getByType(IdeManagementExtension.class);

            //Only register when an IDE import is in progress
            if (ideManagementExtension.isIdeImportInProgress()) {
                //Register both repository copy tasks.
                //TODO: Test if we actually need the raw task here. It should already be executed when IDEA resolves the configuration.
                ideManagementExtension.registerTaskToRun(rawTask);

                //Check if we even have a source jar!
                if (sourceTask != null)
                    ideManagementExtension.registerTaskToRun(sourceTask);

                //As well as all other additional tasks needed.
                result.getAdditionalIdePostSyncTasks().forEach(ideManagementExtension::registerTaskToRun);
            }
        }
        return rawTask;
    }

    /**
     * Create or lookup the task that selects the source artifact from the dependency and puts it in the Ivy repository.
     *
     * @param dependency The dependency that is being replaced.
     * @param result The replacement result from one of the handlers.
     * @param sourceArtifactSelectorName The name of the task that selects the source artifact.
     * @param newRepoEntry The new repository entry that the dependency is being replaced with.
     * @return The task that selects the source artifact from the dependency and puts it in the Ivy repository.
     */
    private TaskProvider<? extends WithOutput> createOrLookupSourcesTask(Dependency dependency, ReplacementResult result, String sourceArtifactSelectorName, Entry newRepoEntry) {
        //Check if we already have this task.
        if (project.getTasks().getNames().contains(sourceArtifactSelectorName)) {
            //Return existing task.
            return project.getTasks().named(sourceArtifactSelectorName, WithOutput.class);
        }

        //Create a new task, using the repository to create the output.
        final Repository repository = project.getExtensions().getByType(Repository.class);
        return project.getTasks().register(sourceArtifactSelectorName, ArtifactFromOutput.class, artifactFromOutput -> {
            artifactFromOutput.setGroup("neogradle/dependencies");
            artifactFromOutput.setDescription(String.format("Selects the source artifact from the %s dependency and puts it in the Ivy repository", dependency));

            if (result.getSourcesJar() != null) {
                artifactFromOutput.getInput().set(result.getSourcesJar().flatMap(WithOutput::getOutput));
            }
            artifactFromOutput.getOutput().set(repository.createOutputFor(newRepoEntry, Repository.Variant.SOURCES_CLASSIFIER));
            artifactFromOutput.dependsOn(result.getSourcesJar());
        });
    }

    /**
     * Create or lookup the task that selects the raw artifact from the dependency and puts it in the Ivy repository.
     *
     * @param dependency The dependency that is being replaced.
     * @param result The replacement result from one of the handlers.
     * @param rawArtifactSelectorName The name of the task that selects the raw artifact.
     * @param newRepoEntry The new repository entry that the dependency is being replaced with.
     * @return The task that selects the raw artifact from the dependency and puts it in the Ivy repository.
     */
    private TaskProvider<? extends WithOutput> createOrLookupRawTask(Dependency dependency, ReplacementResult result, String rawArtifactSelectorName, Entry newRepoEntry) {
        // Check if we already have this task.
        if (project.getTasks().getNames().contains(rawArtifactSelectorName)) {
            // Return existing task.
            return project.getTasks().named(rawArtifactSelectorName, WithOutput.class);
        }

        // Create a new task, using the repository to create the output.
        final Repository repository = project.getExtensions().getByType(Repository.class);
        return project.getTasks().register(rawArtifactSelectorName, ArtifactFromOutput.class, artifactFromOutput -> {
            artifactFromOutput.setGroup("neogradle/dependencies");
            artifactFromOutput.setDescription(String.format("Selects the raw artifact from the %s dependency and puts it in the Ivy repository", dependency));

            artifactFromOutput.getInput().set(result.getRawJar().flatMap(WithOutput::getOutput));
            artifactFromOutput.getOutput().set(repository.createOutputFor(newRepoEntry, Repository.Variant.RETAINED_CLASSIFIER));
            artifactFromOutput.dependsOn(result.getRawJar());
        });
    }

    /**
     * Create a dummy dependency in the dynamic repository for the given dependency and replacement result.
     *
     * @param dependency The dependency that is being replaced.
     * @param result The replacement result from one of the handlers.
     * @return The new repository entry that the dependency is being replaced with.
     */
    @VisibleForTesting
    Entry createDummyDependency(final Dependency dependency, final ReplacementResult result) {
        // Check if the dependency is an external module dependency.
        if (!(dependency instanceof ExternalModuleDependency externalModuleDependency)) {
            // Only ExternalModuleDependency is supported for dependency replacement.
            throw new IllegalStateException("Only ExternalModuleDependency is supported for dependency replacement");
        }

        //Check if we already have a repository entry for this dependency.
        if (repositoryEntries.contains(dependency, result)) {
            return repositoryEntries.get(dependency, result);
        }

        // Create a new repository entry for the dependency, using the replacement result.
        //Check if the result is replacement aware.
        if (result instanceof ReplacementAware replacementAware) {
            //Let it alter the dependency, this allows support for version ranges, and strict versioning.
            externalModuleDependency = replacementAware.getReplacementDependency(externalModuleDependency);
        }

        //Construct a new entry
        final Repository extension = project.getExtensions().getByType(Repository.class);
        final Entry entry = extension.withEntry(
                project.getObjects().newInstance(
                        RepoEntryDefinition.class,
                        project,
                        externalModuleDependency,
                        result.getDependencies(),
                        result.getSourcesJar() != null
                )
        );

        //Store it so that we do not rebuild it.
        repositoryEntries.put(dependency, result, entry);

        return entry;
    }

    public Provider<ConfigurableFileCollection> createDependencyFromTask(TaskProvider<? extends WithOutput> task) {
        return task.map(taskWithOutput -> project.files(taskWithOutput.getOutput()));
    }
}
