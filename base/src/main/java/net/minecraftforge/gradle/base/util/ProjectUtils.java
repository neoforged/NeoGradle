package net.minecraftforge.gradle.base.util;

import net.minecraftforge.gradle.base.extensions.ProjectEvaluationExtension;
import org.gradle.api.Project;

public final class ProjectUtils {

    private ProjectUtils() {
        throw new IllegalStateException("Can not instantiate an instance of: ProjectUtils. This is a utility class");
    }

    public static void afterEvaluate(Project project, Runnable runnable) {
        final ProjectEvaluationExtension extension = project.getExtensions().getByType(ProjectEvaluationExtension.class);
        extension.afterEvaluate(runnable);
    }
}
