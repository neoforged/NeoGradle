package net.neoforged.gradle.eclipse;

import org.gradle.api.Project;
import org.gradle.plugins.ide.eclipse.model.EclipseModel;

import java.io.File;

/**
 * Special metadata reader for Eclipse.
 * Adapted for Gradle 13, which is as of writing the default supported Gradle version.
 */
public class EclipseMetadataReader
{

    /**
     * Called during project apply of the plugin.
     * @param project the project.
     */
    public static void init(Project project) {
        project.getLogger().debug("Loading Eclipse metadata from project {} for Gradle Version 8.13", project.getName());
    }

    /**
     * The core base source output for source sets within an eclipse project.
     *
     * @param project The project in which the eclipse source output is looked up for.
     * @return The file provider that targets the output directory.
     */
    public static IPropertyDelegate<File> getBaseSourceOutputDirFor(Project project) {
        final EclipseModel eclipseModel = project.getExtensions().getByType(EclipseModel.class);
        //In gradle 14 the base source directory is a DirectoryProperty
        return new IPropertyDelegate<File>() {
            @Override
            public File get()
            {
                return eclipseModel.getClasspath().getBaseSourceOutputDir().get();
            }

            @Override
            public void set(final File file)
            {
                eclipseModel.getClasspath().getBaseSourceOutputDir().set(file);
            }

            @Override
            public void convention(final File provider)
            {
                eclipseModel.getClasspath().getBaseSourceOutputDir().convention(provider);
            }
        };
    }
}
