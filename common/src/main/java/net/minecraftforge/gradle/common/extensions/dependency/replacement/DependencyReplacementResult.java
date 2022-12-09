package net.minecraftforge.gradle.common.extensions.dependency.replacement;

import net.minecraftforge.gradle.common.repository.IvyDummyRepositoryEntry;
import net.minecraftforge.gradle.common.tasks.ITaskWithOutput;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
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
        private final Provider<? extends ITaskWithOutput> sourcesJarTaskProvider;
        private final Provider<? extends ITaskWithOutput> rawJarTaskProvider;
        private final Configuration additionalDependenciesConfiguration;
        private final Consumer<IvyDummyRepositoryEntry.Builder> dependencyMetadataConfigurator;
        private final Collection<DependencyReplacementResult> additionalReplacements;

        public DependencyReplacementResult(
                Project project,
                Function<String, String> taskNameBuilder,
                Provider<? extends ITaskWithOutput> sourcesJarTaskProvider,
                Provider<? extends ITaskWithOutput> rawJarTaskProvider,
                Configuration additionalDependenciesConfiguration,
                Consumer<IvyDummyRepositoryEntry.Builder> dependencyMetadataConfigurator
        ) {
                this.project = project;
                this.taskNameBuilder = taskNameBuilder;
                this.sourcesJarTaskProvider = sourcesJarTaskProvider;
                this.rawJarTaskProvider = rawJarTaskProvider;
                this.additionalDependenciesConfiguration = additionalDependenciesConfiguration;
                this.dependencyMetadataConfigurator = dependencyMetadataConfigurator;
                this.additionalReplacements = Collections.emptyList();
        }

        public DependencyReplacementResult(Project project, Function<String, String> taskNameBuilder, Provider<? extends ITaskWithOutput> sourcesJarTaskProvider, Provider<? extends ITaskWithOutput> rawJarTaskProvider, Configuration additionalDependenciesConfiguration, Consumer<IvyDummyRepositoryEntry.Builder> dependencyMetadataConfigurator, Collection<DependencyReplacementResult> additionalReplacements) {
                this.project = project;
                this.taskNameBuilder = taskNameBuilder;
                this.sourcesJarTaskProvider = sourcesJarTaskProvider;
                this.rawJarTaskProvider = rawJarTaskProvider;
                this.additionalDependenciesConfiguration = additionalDependenciesConfiguration;
                this.dependencyMetadataConfigurator = dependencyMetadataConfigurator;
                this.additionalReplacements = additionalReplacements;
        }

        public Project project() {
                return project;
        }

        public Function<String, String> taskNameBuilder() {
                return taskNameBuilder;
        }

        public Provider<? extends ITaskWithOutput> sourcesJarTaskProvider() {
                return sourcesJarTaskProvider;
        }

        public Provider<? extends ITaskWithOutput> rawJarTaskProvider() {
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
