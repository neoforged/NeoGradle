package net.neoforged.gradle.common.extensions;

import org.gradle.api.Action;
import org.gradle.api.Project;

import javax.inject.Inject;

public class ProjectEvaluationExtension {

    private final Project project;
    private boolean isEvaluated = false;

    @Inject
    public ProjectEvaluationExtension(Project project) {
        this.project = project;

        this.project.afterEvaluate(project1 -> setEvaluated());
    }

    public Project getProject() {
        return project;
    }

    public void afterEvaluate(Action<? super Project> action) {
        if (isEvaluated()) {
            action.execute(getProject());
        } else {
            getProject().afterEvaluate(action);
        }
    }

    private boolean isEvaluated() {
        return isEvaluated;
    }

    private void setEvaluated() {
        this.isEvaluated = true;
    }
}
