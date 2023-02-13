package net.minecraftforge.gradle.vanilla;

import net.minecraftforge.gradle.common.CommonPlugin;
import net.minecraftforge.gradle.dsl.common.util.Constants;
import net.minecraftforge.gradle.vanilla.dependency.VanillaDependencyManager;
import net.minecraftforge.gradle.vanilla.runtime.extensions.VanillaRuntimeExtension;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class VanillaProjectPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.getPlugins().apply(CommonPlugin.class);

        VanillaRuntimeExtension runtimeExtension = project.getExtensions().create("vanillaRuntimes", VanillaRuntimeExtension.class, project);

        //Setup handling of the dependencies
        VanillaDependencyManager.getInstance().apply(project);

        //Add Known repos, -> The default tools come from this repo.
        project.getRepositories().maven(e -> {
            e.setUrl(Constants.FORGE_MAVEN);
            e.metadataSources(m -> {
                m.mavenPom();
                m.artifact();
            });
        });
    }
}
