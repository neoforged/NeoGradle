package net.neoforged.gradle.mixin;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class MixinPlugin implements Plugin<Object> {

    @Override
    public void apply(Object o) {
        if (o instanceof Project) {
            final Project project = (Project) o;
            project.getPlugins().apply(MixinProjectPlugin.class);
        }
    }
}