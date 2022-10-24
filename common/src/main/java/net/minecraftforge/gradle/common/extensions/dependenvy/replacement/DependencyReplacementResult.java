package net.minecraftforge.gradle.common.extensions.dependenvy.replacement;

import net.minecraftforge.gradle.common.repository.IvyDummyRepositoryEntry;
import net.minecraftforge.gradle.common.tasks.ITaskWithOutput;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.tasks.TaskProvider;

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

        public DependencyReplacementResult(
                Project project,
                Function<String, String> taskNameBuilder,

                TaskProvider<? extends ITaskWithOutput> sourcesJarTaskProvider,
                TaskProvider<? extends ITaskWithOutput> rawJarTaskProvider,
                Configuration additionalDependenciesConfiguration,
                Consumer<IvyDummyRepositoryEntry.Builder> dependencyMetadataConfigurator
        ) {
                this.project = project;
                this.taskNameBuilder = taskNameBuilder;
                this.sourcesJarTaskProvider = sourcesJarTaskProvider;
                this.rawJarTaskProvider = rawJarTaskProvider;
                this.additionalDependenciesConfiguration = additionalDependenciesConfiguration;
                this.dependencyMetadataConfigurator = dependencyMetadataConfigurator;
        }

        public DependencyReplacementResult(Project project, Function<String, String> taskNameBuilder, TaskProvider<? extends ITaskWithOutput> sourcesJarTaskProvider, TaskProvider<? extends ITaskWithOutput> rawJarTaskProvider, Consumer<IvyDummyRepositoryEntry.Builder> dependencyMetadataConfigurator) {
                this(project, taskNameBuilder, sourcesJarTaskProvider, rawJarTaskProvider, project.getConfigurations().detachedConfiguration(), dependencyMetadataConfigurator);
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

        @Override
        public boolean equals(Object obj) {
                if (obj == this) return true;
                if (obj == null || obj.getClass() != this.getClass()) return false;
                final DependencyReplacementResult that = (DependencyReplacementResult) obj;
                return Objects.equals(this.project, that.project) &&
                        Objects.equals(this.taskNameBuilder, that.taskNameBuilder) &&
                        Objects.equals(this.sourcesJarTaskProvider, that.sourcesJarTaskProvider) &&
                        Objects.equals(this.rawJarTaskProvider, that.rawJarTaskProvider) &&
                        Objects.equals(this.additionalDependenciesConfiguration, that.additionalDependenciesConfiguration) &&
                        Objects.equals(this.dependencyMetadataConfigurator, that.dependencyMetadataConfigurator);
        }

        @Override
        public int hashCode() {
                return Objects.hash(project, taskNameBuilder, sourcesJarTaskProvider, rawJarTaskProvider, additionalDependenciesConfiguration, dependencyMetadataConfigurator);
        }

        @Override
        public String toString() {
                return "DependencyReplacementResult[" +
                        "project=" + project + ", " +
                        "taskNameBuilder=" + taskNameBuilder + ", " +
                        "sourcesJarTaskProvider=" + sourcesJarTaskProvider + ", " +
                        "rawJarTaskProvider=" + rawJarTaskProvider + ", " +
                        "additionalDependenciesConfiguration=" + additionalDependenciesConfiguration + ", " +
                        "dependencyMetadataConfigurator=" + dependencyMetadataConfigurator + ']';
        }

}
