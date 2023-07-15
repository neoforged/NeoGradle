package net.neoforged.gradle.vanilla;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class VanillaPlugin implements Plugin<Object> {

    @Override
    public void apply(Object o) {
        if (o instanceof Project) {
            final Project project = (Project) o;
            project.getPlugins().apply(VanillaProjectPlugin.class);
        }
    }
}
