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
import org.gradle.api.GradleException;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;

/**
 * Defines the implementation of the @{link DependencyReplacement} extension.
 * <p>
 * Uses the configuration system to handle dependency replacement.
 */
public abstract class ReplacementLogic implements ConfigurableDSLElement<DependencyReplacement>, DependencyReplacement {

    private final Project project;

    private final Table<Dependency, Configuration, Optional<ReplacementResult>> dependencyReplacementInformation = HashBasedTable.create();
    private final Table<Dependency, Configuration, TaskProvider<?>> rawJarTasks = HashBasedTable.create();
    private final Table<Dependency, Configuration, TaskProvider<?>> sourceJarTasks = HashBasedTable.create();
    private final NamedDomainObjectContainer<DependencyReplacementHandler> dependencyReplacementHandlers;

    @Inject
    public ReplacementLogic(Project project) {
        this.project = project;

        //Wire up a replacement handler to each configuration for when a dependency is added.
        this.project.getConfigurations().configureEach(this::handleConfiguration);
        //Collection holder of all custom dependency replacement handlers.
        this.dependencyReplacementHandlers = this.project.getObjects().domainObjectContainer(DependencyReplacementHandler.class, name -> this.project.getObjects().newInstance(Handler.class, this.project, name));
    }

    @Override
    public void handleConfiguration(Configuration configuration) {
        //TODO: Figure out if there is any way to do this lazily.
        //TODO: Configure each runs in an immutable context, so we can't add a listener to the dependencies.
        configuration.getDependencies().whenObjectAdded(dependency -> {
            //We need to check if our configuration is unhandled, we can only do this here and not in the register because of way we register unhandled configurations after their creation:
            //TODO: Find a better way to handle this.
            if (ConfigurationUtils.isUnhandledConfiguration(configuration)) {
                //We don't handle this configuration.
                return;
            }

            //We only support module based dependencies.
            if (dependency instanceof ModuleDependency) {
                final ModuleDependency moduleDependency = (ModuleDependency) dependency;
                project.getLogger().lifecycle("Handling dependency " + moduleDependency.getGroup() + ":" + moduleDependency.getName() + " in configuration " + configuration.getName());
                //Try replacing the dependency.
                handleDependency(configuration, moduleDependency);
            }
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
    public Dependency getRawJarDependency(Dependency dependency, Configuration configuration) {
        final TaskProvider<?> taskProvider = rawJarTasks.get(dependency, configuration);
        if (taskProvider == null) {
            throw new IllegalStateException("No raw jar task found for dependency " + dependency + " in configuration " + configuration);
        }

        return createDependencyFromTask(taskProvider);
    }

    @NotNull
    @Override
    public Dependency getSourcesJarDependency(Dependency dependency, Configuration configuration) {
        final TaskProvider<?> taskProvider = sourceJarTasks.get(dependency, configuration);
        if (taskProvider == null) {
            throw new IllegalStateException("No sources jar task found for dependency " + dependency + " in configuration " + configuration);
        }

        return createDependencyFromTask(taskProvider);
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
            if (candidate == null || !candidate.isPresent()) {
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

        //Check if we have a candidate
        if(candidate.isPresent()) {
            //We have a candidate, handle the replacement.
            final ReplacementResult result = candidate.get();
            handleDependencyReplacement(configuration, dependency, result);
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
    @VisibleForTesting
    void handleDependencyReplacement(Configuration configuration, Dependency dependency, ReplacementResult result) {
        //Remove the initial dependency.
        configuration.getDependencies().remove(dependency);

        //Find the configurations that the dependency should be replaced in.
        final List<Configuration> targetConfigurations = ConfigurationUtils.findReplacementConfigurations(project, configuration);

        //Create a new dependency in our dummy repo
        final Entry newRepoEntry = createDummyDependency(dependency, result);

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
        if (result instanceof ReplacementAware) {
            final ReplacementAware replacementAware = (ReplacementAware) result;
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

        //For each configuration that we target we now need to add the new dependencies to.
        for (Configuration targetConfiguration : targetConfigurations) {
            //Create a dependency from the tasks that copies the raw jar to the repository.
            //The sources jar is not needed here.
            final Dependency replacedDependency = createDependencyFromTask(rawTask);

            //Add the new dependency to the target configuration.
            targetConfiguration.getDependencies().add(newRepoEntry.getDependency());
            targetConfiguration.getDependencies().add(replacedDependency);

            //Store the tasks we generate.
            rawJarTasks.put(dependency, targetConfiguration, rawTask);

            //Check if we have a source task.
            if (sourceTask != null)
                sourceJarTasks.put(dependency, targetConfiguration, sourceTask);
        }

        //We need these as well, in-case somebody only has access to for example 'implementation'
        rawJarTasks.put(dependency, configuration, rawTask);

        //Check if we have a source task.
        if (sourceTask != null)
            sourceJarTasks.put(dependency, configuration, sourceTask);
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

            artifactFromOutput.getInput().set(result.getSourcesJar().flatMap(WithOutput::getOutput));
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
        if (!(dependency instanceof ExternalModuleDependency)) {
            // Only ExternalModuleDependency is supported for dependency replacement.
            throw new IllegalStateException("Only ExternalModuleDependency is supported for dependency replacement");
        }

        // Create a new repository entry for the dependency, using the replacement result.
        final ExternalModuleDependency externalModuleDependency = (ExternalModuleDependency) dependency;
        final Repository extension = project.getExtensions().getByType(Repository.class);
        return extension.withEntry(
                project.getObjects().newInstance(
                        RepoEntryDefinition.class,
                        project,
                        externalModuleDependency,
                        result.getDependencies(),
                        result.getSourcesJar() != null
                )
        );
    }

    public Dependency createDependencyFromTask(TaskProvider<? extends Task> task) {
        return this.getProject().getDependencies().create(this.project.files(task));
    }
}
