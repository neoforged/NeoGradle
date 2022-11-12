package net.minecraftforge.gradle.vanilla;

import net.minecraftforge.gradle.common.CommonPlugin;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class VanillaProjectPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.getPlugins().apply(CommonPlugin.class);
    }
}
