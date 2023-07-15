package net.neoforged.gradle.common.util;

import net.neoforged.gradle.common.extensions.ProjectEvaluationExtension;
import org.gradle.api.Project;

public final class ProjectUtils {

    private ProjectUtils() {
        throw new IllegalStateException("Can not instantiate an instance of: ProjectUtils. This is a utility class");
    }

    public static void afterEvaluate(Project project, Runnable runnable) {
        project.getExtensions().getByType(ProjectEvaluationExtension.class).afterEvaluate(project1 -> runnable.run());
    }
}
