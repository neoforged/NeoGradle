package net.minecraftforge.gradle.dsl.common.extensions.dependency.replacement

import groovy.transform.CompileStatic
import net.minecraftforge.gradle.dsl.common.extensions.repository.RepositoryEntry
import net.minecraftforge.gradle.dsl.common.extensions.repository.RepositoryReference
import net.minecraftforge.gradle.dsl.common.tasks.WithOutput
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.annotations.NotNull

import java.util.function.Consumer
import java.util.function.Function

/**
 * Defines a result of a dependency replacement.
 */
@CompileStatic
final class DependencyReplacementResult {
    private final Project project;
    private final Function<String, String> taskNameBuilder;
    private final TaskProvider<? extends WithOutput> sourcesJarTaskProvider;
    private final TaskProvider<? extends WithOutput> rawJarTaskProvider;
    private final Configuration additionalDependenciesConfiguration;
    private final Consumer<RepositoryEntry.Builder<?, ?, ?>> dependencyMetadataConfigurator;
    private final Collection<DependencyReplacementResult> additionalReplacements;
    private final Consumer<RepositoryReference.Builder<?, ?>> asDependencyBuilderConfigurator;
    private final Consumer<Dependency> onCreateReplacedDependencyCallback;

    DependencyReplacementResult(
            Project project,
            Function<String, String> taskNameBuilder,
            TaskProvider<? extends WithOutput> sourcesJarTaskProvider,
            TaskProvider<? extends WithOutput> rawJarTaskProvider,
            Configuration additionalDependenciesConfiguration,
            Consumer<RepositoryEntry.Builder<?, ?, ?>> dependencyMetadataConfigurator,
            Consumer<Dependency> onCreateReplacedDependencyCallback) {
        this.project = project;
        this.taskNameBuilder = taskNameBuilder;
        this.sourcesJarTaskProvider = sourcesJarTaskProvider;
        this.rawJarTaskProvider = rawJarTaskProvider;
        this.additionalDependenciesConfiguration = additionalDependenciesConfiguration;
        this.dependencyMetadataConfigurator = dependencyMetadataConfigurator;
        this.onCreateReplacedDependencyCallback = onCreateReplacedDependencyCallback;
        this.additionalReplacements = Collections.emptyList();

        //TODO: Handle this:
        this.asDependencyBuilderConfigurator = builder -> { };
    }

    DependencyReplacementResult(Project project,
                                Function<String, String> taskNameBuilder,
                                TaskProvider<? extends WithOutput> sourcesJarTaskProvider,
                                TaskProvider<? extends WithOutput> rawJarTaskProvider,
                                Configuration additionalDependenciesConfiguration,
                                Consumer<RepositoryEntry.Builder<?, ?, ?>> dependencyMetadataConfigurator,
                                Collection<DependencyReplacementResult> additionalReplacements,
                                Consumer<RepositoryReference.Builder<?, ?>> asDependencyBuilderConfigurator) {
        this.project = project;
        this.taskNameBuilder = taskNameBuilder;
        this.sourcesJarTaskProvider = sourcesJarTaskProvider;
        this.rawJarTaskProvider = rawJarTaskProvider;
        this.additionalDependenciesConfiguration = additionalDependenciesConfiguration;
        this.dependencyMetadataConfigurator = dependencyMetadataConfigurator;
        this.additionalReplacements = additionalReplacements;
        this.asDependencyBuilderConfigurator = asDependencyBuilderConfigurator;
        this.onCreateReplacedDependencyCallback = dep -> { };
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
     * Gets the configurator which can be used to configure the metadata of the dependency replacement.
     *
     * @return The configurator which can be used to configure the metadata of the dependency replacement.
     */
    @NotNull
    Consumer<RepositoryEntry.Builder<?, ?, ?>> getDependencyMetadataConfigurator() {
        return dependencyMetadataConfigurator;
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
     * Gets the configurator which can be used to configure the dependency replacement as a dependency.
     *
     * @return The configurator which can be used to configure the dependency replacement as a dependency.
     */
    @NotNull
    Consumer<RepositoryReference.Builder<?, ?>> getDependencyBuilderConfigurator() {
        return asDependencyBuilderConfigurator;
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

    @Override
    boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DependencyReplacementResult that = (DependencyReplacementResult) o;
        return Objects.equals(project, that.project) && Objects.equals(taskNameBuilder, that.taskNameBuilder) && Objects.equals(sourcesJarTaskProvider, that.sourcesJarTaskProvider) && Objects.equals(rawJarTaskProvider, that.rawJarTaskProvider) && Objects.equals(additionalDependenciesConfiguration, that.additionalDependenciesConfiguration) && Objects.equals(dependencyMetadataConfigurator, that.dependencyMetadataConfigurator) && Objects.equals(additionalReplacements, that.additionalReplacements);
    }

    @Override
    int hashCode() {
        return Objects.hash(project, taskNameBuilder, sourcesJarTaskProvider, rawJarTaskProvider, additionalDependenciesConfiguration, dependencyMetadataConfigurator, additionalReplacements);
    }

    @Override
    String toString() {
        return "DependencyReplacementResult{" +
                "project=" + project +
                ", taskNameBuilder=" + taskNameBuilder +
                ", sourcesJarTaskProvider=" + sourcesJarTaskProvider +
                ", rawJarTaskProvider=" + rawJarTaskProvider +
                ", additionalDependenciesConfiguration=" + additionalDependenciesConfiguration +
                ", dependencyMetadataConfigurator=" + dependencyMetadataConfigurator +
                ", additionalReplacements=" + additionalReplacements +
                '}';
    }
}
