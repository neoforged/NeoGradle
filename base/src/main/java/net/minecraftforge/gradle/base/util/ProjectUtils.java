package net.minecraftforge.gradle.base.util;

import net.minecraftforge.gradle.base.extensions.ProjectEvaluationExtension;
import org.gradle.api.Project;

/**
 * Utility class for handling projects.
 */
public final class ProjectUtils {

    private ProjectUtils() {
        throw new IllegalStateException("Can not instantiate an instance of: ProjectUtils. This is a utility class");
    }

    /**
     * Runs the given runnable after the project has been evaluated.
     * The runnable will be run immediately if the project has already been evaluated.
     *
     * @param project The project to run the runnable after it has been evaluated.
     * @param runnable The runnable to run after the project has been evaluated.
     * @see ProjectEvaluationExtension#afterEvaluate(Runnable)
     */
    public static void afterEvaluate(Project project, Runnable runnable) {
        final ProjectEvaluationExtension extension = project.getExtensions().getByType(ProjectEvaluationExtension.class);
        extension.afterEvaluate(runnable);
    }
}
