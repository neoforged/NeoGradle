package net.neoforged.gradle.common.extensions.dependency.creation;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.tasks.TaskProvider;

import javax.inject.Inject;

/**
 * A project based dependency creator.
 * @implNote This is the default implementation. This abstraction exists so that consumers can be tested properly.
 */
public abstract class ProjectBasedDependencyCreator implements DependencyCreator {

    private final Project project;

    @Inject
    public ProjectBasedDependencyCreator(Project project) {
        this.project = project;
    }

    public Project getProject() {
        return project;
    }

    @Override
    public Dependency from(TaskProvider<? extends Task> task) {
        return this.getProject().getDependencies().create(this.project.files(task));
    }
}
