package net.neoforged.gradle.common.util;

import net.neoforged.gradle.dsl.common.extensions.ProjectHolder;
import net.neoforged.gradle.dsl.common.extensions.RunnableSourceSet;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;

import java.util.HashSet;
import java.util.Set;

public class SourceSetUtils {
    
    private SourceSetUtils() {
        throw new IllegalStateException("Tried to create utility class!");
    }
    
    public static Project getProject(final SourceSet sourceSet) {
        final ProjectHolder projectHolder = sourceSet.getExtensions().findByType(ProjectHolder.class);
        if (projectHolder != null) {
            projectHolder.getProject();
        }
        
        final Iterable<? extends Task> tasks = sourceSet.getOutput().getBuildDependencies().getDependencies(null);
        final Set<Project> projects = new HashSet<>();
        for (final Task task : tasks) {
            final Project project = task.getProject();
            projects.add(project);
        }
        
        projects.removeIf(project -> !project.getExtensions().getByType(SourceSetContainer.class).contains(sourceSet));
        
        if (projects.size() == 1) {
            return projects.iterator().next();
        }
        
        throw new IllegalStateException("Could not find project for source set " + sourceSet.getName());
    }
    
    public static String getModIdentifier(final SourceSet sourceSet) {
        final RunnableSourceSet runnableSourceSet = sourceSet.getExtensions().findByType(RunnableSourceSet.class);
        if (runnableSourceSet != null)
            return runnableSourceSet.getModIdentifier().get();
        
        return getProject(sourceSet).getName();
    }
}
