package net.neoforged.gradle.userdev;

import net.neoforged.gradle.common.extensions.DefaultJarJarFeature;
import net.neoforged.gradle.common.extensions.JarJarExtension;
import net.neoforged.gradle.dsl.common.extensions.JarJar;
import net.neoforged.gradle.neoform.NeoFormPlugin;
import net.neoforged.gradle.userdev.dependency.UserDevDependencyManager;
import net.neoforged.gradle.userdev.runtime.extension.UserDevRuntimeExtension;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class UserDevProjectPlugin implements Plugin<Project> {
    public static final String JAR_JAR_TASK_NAME = DefaultJarJarFeature.JAR_JAR_TASK_NAME;
    public static final String JAR_JAR_GROUP = DefaultJarJarFeature.JAR_JAR_GROUP;

    public static final String JAR_JAR_DEFAULT_CONFIGURATION_NAME = DefaultJarJarFeature.JAR_JAR_DEFAULT_CONFIGURATION_NAME;


    @Override
    public void apply(Project project) {
        project.getPlugins().apply(NeoFormPlugin.class);

        project.getExtensions().create("userDevRuntime", UserDevRuntimeExtension.class, project);

        UserDevDependencyManager.getInstance().apply(project);

        final JarJar jarJar = project.getExtensions().create(JarJar.class, JarJarExtension.EXTENSION_NAME, JarJarExtension.class, project);

        configureJarJarTask(project, jarJar);
    }

    protected void configureJarJarTask(Project project, JarJar jarJarExtension) {
        ((DefaultJarJarFeature) jarJarExtension).createTaskAndConfiguration();
    }
}
