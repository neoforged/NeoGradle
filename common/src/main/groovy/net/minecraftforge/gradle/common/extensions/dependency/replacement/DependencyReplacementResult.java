package net.minecraftforge.gradle.common.extensions.dependency.replacement;

import net.minecraftforge.gradle.common.repository.IvyDummyRepositoryEntry;
import net.minecraftforge.gradle.common.repository.IvyDummyRepositoryEntryDependency;
import net.minecraftforge.gradle.common.tasks.ITaskWithOutput;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public final class DependencyReplacementResult {
        private final Project project;
        private final Function<String, String> taskNameBuilder;
        private final TaskProvider<? extends ITaskWithOutput> sourcesJarTaskProvider;
        private final TaskProvider<? extends ITaskWithOutput> rawJarTaskProvider;
        private final Configuration additionalDependenciesConfiguration;
        private final Consumer<IvyDummyRepositoryEntry.Builder> dependencyMetadataConfigurator;
        private final Collection<DependencyReplacementResult> additionalReplacements;
        private final Consumer<IvyDummyRepositoryEntryDependency.Builder> asDependencyBuilderConfigurator;
        private final Consumer<Dependency> onCreateReplacedDependencyCallback;

        public DependencyReplacementResult(
                Project project,
                Function<String, String> taskNameBuilder,
                TaskProvider<? extends ITaskWithOutput> sourcesJarTaskProvider,
                TaskProvider<? extends ITaskWithOutput> rawJarTaskProvider,
                Configuration additionalDependenciesConfiguration,
                Consumer<IvyDummyRepositoryEntry.Builder> dependencyMetadataConfigurator,
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
                this.asDependencyBuilderConfigurator = builder -> {};
        }

        public DependencyReplacementResult(Project project, Function<String, String> taskNameBuilder, TaskProvider<? extends ITaskWithOutput> sourcesJarTaskProvider, TaskProvider<? extends ITaskWithOutput> rawJarTaskProvider, Configuration additionalDependenciesConfiguration, Consumer<IvyDummyRepositoryEntry.Builder> dependencyMetadataConfigurator, Collection<DependencyReplacementResult> additionalReplacements, Consumer<IvyDummyRepositoryEntryDependency.Builder> asDependencyBuilderConfigurator) {
                this.project = project;
                this.taskNameBuilder = taskNameBuilder;
                this.sourcesJarTaskProvider = sourcesJarTaskProvider;
                this.rawJarTaskProvider = rawJarTaskProvider;
                this.additionalDependenciesConfiguration = additionalDependenciesConfiguration;
                this.dependencyMetadataConfigurator = dependencyMetadataConfigurator;
                this.additionalReplacements = additionalReplacements;
                this.asDependencyBuilderConfigurator = asDependencyBuilderConfigurator;
                this.onCreateReplacedDependencyCallback = dep -> {};
        }

        public Project project() {
                return project;
        }

        public Function<String, String> taskNameBuilder() {
                return taskNameBuilder;
        }

        public TaskProvider<? extends ITaskWithOutput> sourcesJarTaskProvider() {
                return sourcesJarTaskProvider;
        }

        public TaskProvider<? extends ITaskWithOutput> rawJarTaskProvider() {
                return rawJarTaskProvider;
        }

        public Configuration additionalDependenciesConfiguration() {
                return additionalDependenciesConfiguration;
        }

        public Consumer<IvyDummyRepositoryEntry.Builder> dependencyMetadataConfigurator() {
                return dependencyMetadataConfigurator;
        }

        public Collection<DependencyReplacementResult> additionalReplacements() {
                return additionalReplacements;
        }

        public Consumer<IvyDummyRepositoryEntryDependency.Builder> asDependencyBuilderConfigurator() {
                return asDependencyBuilderConfigurator;
        }

        public Consumer<Dependency> onCreateReplacedDependencyCallback() {
                return onCreateReplacedDependencyCallback;
        }

        @Override
        public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                DependencyReplacementResult that = (DependencyReplacementResult) o;
                return Objects.equals(project, that.project) && Objects.equals(taskNameBuilder, that.taskNameBuilder) && Objects.equals(sourcesJarTaskProvider, that.sourcesJarTaskProvider) && Objects.equals(rawJarTaskProvider, that.rawJarTaskProvider) && Objects.equals(additionalDependenciesConfiguration, that.additionalDependenciesConfiguration) && Objects.equals(dependencyMetadataConfigurator, that.dependencyMetadataConfigurator) && Objects.equals(additionalReplacements, that.additionalReplacements);
        }

        @Override
        public int hashCode() {
                return Objects.hash(project, taskNameBuilder, sourcesJarTaskProvider, rawJarTaskProvider, additionalDependenciesConfiguration, dependencyMetadataConfigurator, additionalReplacements);
        }

        @Override
        public String toString() {
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
