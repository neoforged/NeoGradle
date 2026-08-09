package net.neoforged.gradle.common.util;

import net.neoforged.gradle.dsl.common.extensions.ProjectHolder;
import net.neoforged.gradle.dsl.common.extensions.sourceset.RunnableSourceSet;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

public class SourceSetUtils {
    
    private SourceSetUtils() {
        throw new IllegalStateException("Tried to create utility class!");
    }
    
    public static Project getProject(final SourceSet sourceSet) {
        final ProjectHolder projectHolder = sourceSet.getExtensions().findByType(ProjectHolder.class);
        if (projectHolder != null) {
            return projectHolder.getProject();
        }

        //NeoGradle always registers ProjectHolder on its source sets via CommonProjectPlugin.
        //If we reach here, this is a non-NeoGradle source set — throw a clear error instead of
        //falling back to getBuildDependencies().getDependencies() which violates isolated projects.
        throw new IllegalStateException("Could not find project for source set " + sourceSet.getName()
            + ". Source sets must be managed by NeoGradle's CommonProjectPlugin (which registers ProjectHolder).");
    }
    
    public static String getModIdentifier(final SourceSet sourceSet, final @Nullable Project project) {
        final RunnableSourceSet runnableSourceSet = sourceSet.getExtensions().findByType(RunnableSourceSet.class);
        if (runnableSourceSet != null)
            return runnableSourceSet.getModIdentifier().get();

        if (project == null) {
            return getProject(sourceSet).getName();
        } else {
            return project.getName();
        }
    }


}
