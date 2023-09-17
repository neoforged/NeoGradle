package net.neoforged.gradle.platform.util;

import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;

import java.io.File;

public class SetupUtils {
    
    private SetupUtils() {
        throw new IllegalStateException("Tried to create utility class!");
    }
    
    public static File getSetupSourceTarget(final Project project) {
        final JavaPluginExtension javaPluginExtension = project.getExtensions().getByType(JavaPluginExtension.class);
        final SourceSet mainSource = javaPluginExtension.getSourceSets().getByName("main");
        return mainSource.getJava().getFiles().size() == 1 ? mainSource.getJava().getSourceDirectories().getSingleFile() : project.file("src/main/java");
    }
    
    public static File getSetupResourcesTarget(final Project project) {
        final JavaPluginExtension javaPluginExtension = project.getExtensions().getByType(JavaPluginExtension.class);
        final SourceSet mainSource = javaPluginExtension.getSourceSets().getByName("main");
        return mainSource.getResources().getFiles().size() == 1 ? mainSource.getResources().getSourceDirectories().getSingleFile() : project.file("src/main/resources");
    }
}
