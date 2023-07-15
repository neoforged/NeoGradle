package net.neoforged.gradle.common;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

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
