package net.minecraftforge.gradle.userdev;

import net.minecraftforge.gradle.dsl.userdev.extension.UserDev;
import net.minecraftforge.gradle.mcp.McpPlugin;
import net.minecraftforge.gradle.userdev.dependency.UserDevDependencyManager;
import net.minecraftforge.gradle.userdev.extension.UserDevExtension;
import net.minecraftforge.gradle.userdev.runtime.extension.UserDevRuntimeExtension;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class UserDevProjectPlugin implements Plugin<Project> {


    @Override
    public void apply(Project project) {
        project.getPlugins().apply(McpPlugin.class);

        project.getExtensions().create(UserDev.class, "userDev", UserDevExtension.class, project);
        project.getExtensions().create("userDevRuntime", UserDevRuntimeExtension.class, project);

        UserDevDependencyManager.getInstance().apply(project);
    }
}
