package net.minecraftforge.gradle.userdev;

import net.minecraftforge.gradle.mcp.McpPlugin;
import net.minecraftforge.gradle.userdev.dependency.ForgeUserDevDependencyManager;
import net.minecraftforge.gradle.userdev.extension.ForgeUserDevExtension;
import net.minecraftforge.gradle.userdev.runtime.extension.ForgeUserDevRuntimeExtension;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class UserDevProjectPlugin implements Plugin<Project> {


    @Override
    public void apply(Project project) {
        project.getPlugins().apply(McpPlugin.class);

        project.getExtensions().create("forge", ForgeUserDevExtension.class, project);
        project.getExtensions().create("forgeUserDevRuntime", ForgeUserDevRuntimeExtension.class, project);

        ForgeUserDevDependencyManager.getInstance().apply(project);
    }
}
