package net.neoforged.gradle.common;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.jetbrains.annotations.NotNull;

public class CommonPlugin implements Plugin<Object> {
    @Override
    public void apply(@NotNull Object o) {
        if (o instanceof Project project) {
            project.getPluginManager().apply(CommonProjectPlugin.class);
        } else {
            throw new IllegalArgumentException("CommonPlugin can only be applied to a project");
        }
    }
}
