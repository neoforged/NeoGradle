package net.neoforged.gradle.legacy;

import net.neoforged.gradle.common.extensions.ExtensionManager;
import net.neoforged.gradle.legacy.extensions.LegacyForgeGradleExtension;
import net.neoforged.gradle.legacy.extensions.LegacyMinecraftExtension;
import net.neoforged.gradle.legacy.tasks.NoopLegacyRenameJarInPlace;
import net.neoforged.gradle.userdev.UserDevPlugin;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;

@Deprecated
public class LegacyProjectPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        //We need to override the default minecraft extension
        ExtensionManager.registerOverride(project, "minecraft", new LegacyMinecraftExtension.Creator());

        project.getPlugins().apply(UserDevPlugin.class);

        project.getExtensions().create("fg", LegacyForgeGradleExtension.class, project);

        project.getConfigurations().create("minecraft");
        project.getConfigurations().named("implementation").configure(implementationConfiguration -> {
            implementationConfiguration.extendsFrom(project.getConfigurations().getByName("minecraft"));
        });

        project.getTasks().register("reobfJar", NoopLegacyRenameJarInPlace.class);

        registerReobfuscationExtension(project);
    }

    private void registerReobfuscationExtension(Project project) {
        final NamedDomainObjectContainer<NoopLegacyRenameJarInPlace> reobfExtension = project.container(NoopLegacyRenameJarInPlace.class, jarName -> {
            String name = StringUtils.capitalize(jarName);
            return project.getTasks().maybeCreate("reobf" + name, NoopLegacyRenameJarInPlace.class);
        });
        project.getExtensions().add("reobf", reobfExtension);
        reobfExtension.create(JavaPlugin.JAR_TASK_NAME);
    }
}
