package net.minecraftforge.gradle.base.extensions;

import org.gradle.api.Project;

import javax.inject.Inject;

/**
 * An extension that provides a way to run code after the project has been evaluated.
 * Or immediately if the project has already been evaluated.
 *
 * This extension is automatically added to all projects, by the common project.
 */
public abstract class ProjectEvaluationExtension {

    private final Project project;
    private boolean evaluated = false;

    @Inject
    public ProjectEvaluationExtension(Project project) {
        this.project = project;
        this.project.afterEvaluate(p -> evaluated = true);
    }

    /**
     * Gets the project that this extension is attached to.
     *
     * @return The project
     */
    public Project getProject() {
        return project;
    }

    /**
     * Runs the given runnable after the project has been evaluated.
     * If the project has already been evaluated, the runnable is run immediately.
     *
     * @param runnable The runnable to run
     */
    public void afterEvaluate(Runnable runnable) {
        if (evaluated) {
            runnable.run();
        } else {
            project.afterEvaluate(p -> runnable.run());
        }
    }
}
