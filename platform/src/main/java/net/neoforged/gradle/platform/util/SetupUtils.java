package net.neoforged.gradle.platform.util;

import net.neoforged.gradle.common.util.SourceSetUtils;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;

import java.io.File;

public class SetupUtils {
    
    private SetupUtils() {
        throw new IllegalStateException("Tried to create utility class!");
    }
    
    public static File getSetupSourceTarget(final SourceSet sourceSet) {
        final Project project = SourceSetUtils.getProject(sourceSet);
        return sourceSet.getJava().getFiles().size() == 1 ? sourceSet.getJava().getSourceDirectories().getSingleFile() : project.file("src/%s/java".formatted(sourceSet.getName()));
    }

    public static File getSetupResourcesTarget(final SourceSet mainSource) {
        final Project project = SourceSetUtils.getProject(mainSource);
        return mainSource.getResources().getFiles().size() == 1 ? mainSource.getResources().getSourceDirectories().getSingleFile() : project.file("src/%s/resources".formatted(mainSource.getName()));
    }
}
