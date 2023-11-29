package net.neoforged.gradle.userdev;

import net.neoforged.gradle.dsl.userdev.extension.JarJar;
import net.neoforged.gradle.dsl.userdev.extension.UserDev;
import net.neoforged.gradle.neoform.NeoFormPlugin;
import net.neoforged.gradle.userdev.dependency.UserDevDependencyManager;
import net.neoforged.gradle.userdev.extension.UserDevExtension;
import net.neoforged.gradle.userdev.jarjar.JarJarExtension;
import net.neoforged.gradle.userdev.runtime.extension.UserDevRuntimeExtension;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.language.base.plugins.LifecycleBasePlugin;

public class UserDevProjectPlugin implements Plugin<Project> {
    public static final String JAR_JAR_TASK_NAME = "jarJar";
    public static final String JAR_JAR_GROUP = "jarjar";

    public static final String JAR_JAR_DEFAULT_CONFIGURATION_NAME = "jarJar";


    @Override
    public void apply(Project project) {
        project.getPlugins().apply(NeoFormPlugin.class);

        project.getExtensions().create(UserDev.class, "userDev", UserDevExtension.class, project);
        project.getExtensions().create("userDevRuntime", UserDevRuntimeExtension.class, project);

        UserDevDependencyManager.getInstance().apply(project);

        final JarJar jarJar = project.getExtensions().create(JarJar.class, JarJarExtension.EXTENSION_NAME, JarJarExtension.class, project);

        configureJarJarTask(project, jarJar);
    }

    protected void configureJarJarTask(Project project, JarJar jarJarExtension) {
        final Configuration configuration = project.getConfigurations().create(JAR_JAR_DEFAULT_CONFIGURATION_NAME);

        JavaPluginExtension javaPluginExtension = project.getExtensions().getByType(JavaPluginExtension.class);

        TaskProvider<net.neoforged.gradle.userdev.tasks.JarJar> jarJarTask = project.getTasks().register(JAR_JAR_TASK_NAME, net.neoforged.gradle.userdev.tasks.JarJar.class, jarJar -> {
            jarJar.setGroup(JAR_JAR_GROUP);
            jarJar.setDescription("Create a combined JAR of project and selected dependencies");
            jarJar.getArchiveClassifier().convention("all");

            if (!jarJarExtension.getDefaultSourcesDisabled()) {
                jarJar.getManifest().inheritFrom(((Jar) project.getTasks().getByName("jar")).getManifest());
                jarJar.from(javaPluginExtension.getSourceSets().getByName("main").getOutput());
            }

            jarJar.configuration(configuration);

            jarJar.setEnabled(false);
        });

        project.getArtifacts().add(JAR_JAR_DEFAULT_CONFIGURATION_NAME, jarJarTask);

        project.getTasks().named(LifecycleBasePlugin.ASSEMBLE_TASK_NAME, t -> {
            t.dependsOn(jarJarTask);
        });
    }
}
