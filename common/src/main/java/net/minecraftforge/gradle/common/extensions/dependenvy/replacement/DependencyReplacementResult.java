package net.minecraftforge.gradle.common.extensions.dependenvy.replacement;

import net.minecraftforge.gradle.common.repository.IvyDummyRepositoryEntry;
import net.minecraftforge.gradle.common.tasks.ITaskWithOutput;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.tasks.TaskProvider;

import java.util.function.Consumer;
import java.util.function.Function;

public record DependencyReplacementResult(
        Project project,
        Function<String, String> taskNameBuilder,

        TaskProvider<? extends ITaskWithOutput> sourcesJarTaskProvider,
        TaskProvider<? extends ITaskWithOutput> rawJarTaskProvider,
        Configuration additionalDependenciesConfiguration,
        Consumer<IvyDummyRepositoryEntry.Builder> dependencyMetadataConfigurator
        ) {

        public DependencyReplacementResult(Project project, Function<String, String> taskNameBuilder, TaskProvider<? extends ITaskWithOutput> sourcesJarTaskProvider, TaskProvider<? extends ITaskWithOutput> rawJarTaskProvider, Consumer<IvyDummyRepositoryEntry.Builder> dependencyMetadataConfigurator) {
                this(project, taskNameBuilder, sourcesJarTaskProvider, rawJarTaskProvider, project.getConfigurations().detachedConfiguration(), dependencyMetadataConfigurator);
        }
}
