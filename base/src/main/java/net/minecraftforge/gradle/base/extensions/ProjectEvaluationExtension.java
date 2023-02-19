package net.minecraftforge.gradle.base.extensions;

import org.gradle.api.Project;

import javax.inject.Inject;

public abstract class ProjectEvaluationExtension {

    private final Project project;
    private boolean evaluated = false;

    @Inject
    public ProjectEvaluationExtension(Project project) {
        this.project = project;
        this.project.afterEvaluate(p -> evaluated = true);
    }

    public Project getProject() {
        return project;
    }

    public boolean isEvaluated() {
        return evaluated;
    }

    public void afterEvaluate(Runnable runnable) {
        if (evaluated) {
            runnable.run();
        } else {
            project.afterEvaluate(p -> runnable.run());
        }
    }
}
