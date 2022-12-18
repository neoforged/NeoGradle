package net.minecraftforge.gradle.common;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.initialization.Settings;

public class CommonPlugin implements Plugin<Object> {
    @Override
    public void apply(Object o) {
        if (o instanceof Project) {
            final Project project = (Project) o;
            project.getPluginManager().apply(CommonProjectPlugin.class);
        } else {
            throw new IllegalArgumentException("CommonPlugin can only be applied to a project");
        }
    }
}
