package net.neoforged.gradle.dsl.common.extensions.dependency.replacement

import groovy.transform.CompileStatic
import net.neoforged.gradle.dsl.common.extensions.repository.RepositoryEntry
import net.neoforged.gradle.dsl.common.extensions.repository.RepositoryReference
import net.neoforged.gradle.dsl.common.tasks.WithOutput
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.annotations.NotNull

import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Supplier

/**
 * Defines a result of a dependency replacement.
 */
@CompileStatic
final class DependencyReplacementResult {
    private final Project project;
    private final Optional<List<Configuration>> targetConfiguration;
    private final Function<String, String> taskNameBuilder;
    private final TaskProvider<? extends WithOutput> sourcesJarTaskProvider;
    private final TaskProvider<? extends WithOutput> rawJarTaskProvider;
    private final Configuration additionalDependenciesConfiguration;
    private final Consumer<RepositoryReference.Builder<?, ?>> referenceConfigurator;
    private final Consumer<RepositoryEntry.Builder<?, ?, ?>> metadataConfigurator;
    private final Collection<DependencyReplacementResult> additionalReplacements;
    private final Consumer<Dependency> onCreateReplacedDependencyCallback;
    private final Consumer<TaskProvider<? extends WithOutput>> onRepoWritingTaskRegisteredCallback;
    private final Supplier<Set<TaskProvider>> additionalIdePostSyncTasks;
    private final Boolean processImmediately;

    DependencyReplacementResult(
            Project project,
            Optional<List<Configuration>> targetConfiguration,
            Function<String, String> taskNameBuilder,
            TaskProvider<? extends WithOutput> sourcesJarTaskProvider,
            TaskProvider<? extends WithOutput> rawJarTaskProvider,
            Configuration additionalDependenciesConfiguration,
            Consumer<RepositoryReference.Builder<?,?>> referenceConfigurator,
            Consumer<RepositoryEntry.Builder<?, ?, ?>> metadataConfigurator,
            Consumer<Dependency> onCreateReplacedDependencyCallback,
            Consumer<TaskProvider<? extends WithOutput>> onRepoWritingTaskRegisteredCallback,
            Supplier<Set<TaskProvider>> additionalIdePostSyncTasks,
            boolean processImmediately
    ) {
        this.project = project;
        this.targetConfiguration = targetConfiguration;
        this.taskNameBuilder = taskNameBuilder;
        this.sourcesJarTaskProvider = sourcesJarTaskProvider;
        this.rawJarTaskProvider = rawJarTaskProvider;
        this.additionalDependenciesConfiguration = additionalDependenciesConfiguration;
        this.referenceConfigurator = referenceConfigurator;
        this.metadataConfigurator = metadataConfigurator;
        this.onCreateReplacedDependencyCallback = onCreateReplacedDependencyCallback;
        this.onRepoWritingTaskRegisteredCallback = onRepoWritingTaskRegisteredCallback;
        this.additionalReplacements = Collections.emptyList();
        this.additionalIdePostSyncTasks = additionalIdePostSyncTasks;
        this.processImmediately = processImmediately;
    }

    DependencyReplacementResult(
            Project project,
            Function<String, String> taskNameBuilder,
            TaskProvider<? extends WithOutput> sourcesJarTaskProvider,
            TaskProvider<? extends WithOutput> rawJarTaskProvider,
            Configuration additionalDependenciesConfiguration,
            Consumer<RepositoryReference.Builder<?,?>> referenceConfigurator,
            Consumer<RepositoryEntry.Builder<?, ?, ?>> metadataConfigurator,
            Consumer<Dependency> onCreateReplacedDependencyCallback,
            Consumer<TaskProvider<? extends WithOutput>> onRepoWritingTaskRegisteredCallback,
            Supplier<Set<TaskProvider>> additionalIdePostSyncTasks,
            boolean processImmediately
    ) {
        this.project = project;
        this.targetConfiguration = Optional.empty();
        this.taskNameBuilder = taskNameBuilder;
        this.sourcesJarTaskProvider = sourcesJarTaskProvider;
        this.rawJarTaskProvider = rawJarTaskProvider;
        this.additionalDependenciesConfiguration = additionalDependenciesConfiguration;
        this.referenceConfigurator = referenceConfigurator;
        this.metadataConfigurator = metadataConfigurator;
        this.onCreateReplacedDependencyCallback = onCreateReplacedDependencyCallback;
        this.onRepoWritingTaskRegisteredCallback = onRepoWritingTaskRegisteredCallback;
        this.additionalReplacements = Collections.emptyList();
        this.additionalIdePostSyncTasks = additionalIdePostSyncTasks;
        this.processImmediately = processImmediately;
    }

    DependencyReplacementResult(
            Project project,
            Optional<List<Configuration>> targetConfiguration,
            Function<String, String> taskNameBuilder,
            TaskProvider<? extends WithOutput> sourcesJarTaskProvider,
            TaskProvider<? extends WithOutput> rawJarTaskProvider,
            Configuration additionalDependenciesConfiguration,
            Consumer<RepositoryReference.Builder<?,?>> referenceConfigurator,
            Consumer<RepositoryEntry.Builder<?, ?, ?>> metadataConfigurator,
            Consumer<Dependency> onCreateReplacedDependencyCallback,
            Consumer<TaskProvider<? extends WithOutput>> onRepoWritingTaskRegisteredCallback,
            Supplier<Set<TaskProvider>> additionalIdePostSyncTasks) {
        this(
                project,
                targetConfiguration,
                taskNameBuilder,
                sourcesJarTaskProvider,
                rawJarTaskProvider,
                additionalDependenciesConfiguration,
                referenceConfigurator,
                metadataConfigurator,
                onCreateReplacedDependencyCallback,
                onRepoWritingTaskRegisteredCallback,
                additionalIdePostSyncTasks,
                false
        );
    }

    /**
     * Gets the project inside of which a dependency replacement is being performed.
     *
     * @return The project inside of which a dependency replacement is being performed.
     */
    @NotNull
    Project getProject() {
        return project;
    }

    /**
     * Defines the configuration in which the dependency is placed.
     * @return
     */
    @NotNull
    Optional<List<Configuration>> getTargetConfiguration() {
        return targetConfiguration
    }

    /**
     * The function which can build a task name for the current context.
     *
     * @return The function which can build a task name for the current context.
     */
    @NotNull
    Function<String, String> getTaskNameBuilder() {
        return taskNameBuilder;
    }

    /**
     * Gives access to the task which produces the sources jar for the dependency replacement.
     *
     * @return The task which produces the sources jar for the dependency replacement.
     */
    @NotNull
    TaskProvider<? extends WithOutput> getSourcesJarTaskProvider() {
        return sourcesJarTaskProvider;
    }

    /**
     * Gives access to the task which produces the raw jar for the dependency replacement.
     *
     * @return The task which produces the raw jar for the dependency replacement.
     */
    @NotNull
    TaskProvider<? extends WithOutput> getRawJarTaskProvider() {
        return rawJarTaskProvider;
    }

    /**
     * Gets the configuration in which additional dependencies are added.
     *
     * @return The configuration in which additional dependencies are added.
     */
    @NotNull
    Configuration getAdditionalDependenciesConfiguration() {
        return additionalDependenciesConfiguration;
    }

    /**
     * Gets the configurator which can be used to configure the reference of the dependency replacement.
     *
     * @return The configurator which can be used to configure the reference of the dependency replacement.
     */
    @NotNull
    Consumer<RepositoryReference.Builder<?, ?>> getReferenceConfigurator() {
        return referenceConfigurator;
    }

    /**
     * Gets the configurator which can be used to configure the metadata of the dependency replacement.
     *
     * @return The configurator which can be used to configure the metadata of the dependency replacement.
     */
    @NotNull
    Consumer<RepositoryEntry.Builder<?, ?, ?>> getMetadataConfigurator() {
        return metadataConfigurator;
    }

    /**
     * Gets a collection of additional replacements which should be performed.
     *
     * @return A collection of additional replacements which should be performed.
     */
    @NotNull
    Collection<DependencyReplacementResult> getAdditionalReplacements() {
        return additionalReplacements;
    }

    /**
     * Gets the callback which is invoked when a dependency is created for the dependency replacement.
     *
     * @return The callback which is invoked when a dependency is created for the dependency replacement.
     */
    @NotNull
    Consumer<Dependency> getOnCreateReplacedDependencyCallback() {
        return onCreateReplacedDependencyCallback;
    }

    @NotNull
    Consumer<TaskProvider<? extends WithOutput>> getOnRepoWritingTaskRegisteredCallback() {
        return onRepoWritingTaskRegisteredCallback;
    }

    @NotNull
    Set<TaskProvider> getAdditionalIdePostSyncTasks() {
        return additionalIdePostSyncTasks.get();
    }

    Boolean getProcessImmediately() {
        return processImmediately
    }

    boolean equals(o) {
        if (this.is(o)) return true
        if (o == null || getClass() != o.class) return false

        DependencyReplacementResult that = (DependencyReplacementResult) o

        if (additionalDependenciesConfiguration != that.additionalDependenciesConfiguration) return false
        if (additionalIdePostSyncTasks != that.additionalIdePostSyncTasks) return false
        if (additionalReplacements != that.additionalReplacements) return false
        if (metadataConfigurator != that.metadataConfigurator) return false
        if (onCreateReplacedDependencyCallback != that.onCreateReplacedDependencyCallback) return false
        if (onRepoWritingTaskRegisteredCallback != that.onRepoWritingTaskRegisteredCallback) return false
        if (processImmediately != that.processImmediately) return false
        if (project != that.project) return false
        if (rawJarTaskProvider != that.rawJarTaskProvider) return false
        if (referenceConfigurator != that.referenceConfigurator) return false
        if (sourcesJarTaskProvider != that.sourcesJarTaskProvider) return false
        if (taskNameBuilder != that.taskNameBuilder) return false

        return true
    }

    int hashCode() {
        int result
        result = (project != null ? project.hashCode() : 0)
        result = 31 * result + (taskNameBuilder != null ? taskNameBuilder.hashCode() : 0)
        result = 31 * result + (sourcesJarTaskProvider != null ? sourcesJarTaskProvider.hashCode() : 0)
        result = 31 * result + (rawJarTaskProvider != null ? rawJarTaskProvider.hashCode() : 0)
        result = 31 * result + (additionalDependenciesConfiguration != null ? additionalDependenciesConfiguration.hashCode() : 0)
        result = 31 * result + (referenceConfigurator != null ? referenceConfigurator.hashCode() : 0)
        result = 31 * result + (metadataConfigurator != null ? metadataConfigurator.hashCode() : 0)
        result = 31 * result + (additionalReplacements != null ? additionalReplacements.hashCode() : 0)
        result = 31 * result + (onCreateReplacedDependencyCallback != null ? onCreateReplacedDependencyCallback.hashCode() : 0)
        result = 31 * result + (onRepoWritingTaskRegisteredCallback != null ? onRepoWritingTaskRegisteredCallback.hashCode() : 0)
        result = 31 * result + (additionalIdePostSyncTasks != null ? additionalIdePostSyncTasks.hashCode() : 0)
        result = 31 * result + (processImmediately != null ? processImmediately.hashCode() : 0)
        return result
    }


    @Override
    public String toString() {
        return "DependencyReplacementResult{" +
                "project=" + project +
                ", taskNameBuilder=" + taskNameBuilder +
                ", sourcesJarTaskProvider=" + sourcesJarTaskProvider +
                ", rawJarTaskProvider=" + rawJarTaskProvider +
                ", additionalDependenciesConfiguration=" + additionalDependenciesConfiguration +
                ", referenceConfigurator=" + referenceConfigurator +
                ", metadataConfigurator=" + metadataConfigurator +
                ", additionalReplacements=" + additionalReplacements +
                ", onCreateReplacedDependencyCallback=" + onCreateReplacedDependencyCallback +
                ", onRepoWritingTaskRegisteredCallback=" + onRepoWritingTaskRegisteredCallback +
                ", additionalIdePostSyncTasks=" + additionalIdePostSyncTasks +
                ", processImmediately=" + processImmediately +
                '}';
    }
}
