package net.neoforged.gradle.junit;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class JUnitPlugin implements Plugin<Object> {
    @Override
    public void apply(Object o) {
        if (o instanceof Project) {
            final Project project = (Project) o;
            project.getPluginManager().apply(JUnitProjectPlugin.class);
        } else {
            throw new IllegalArgumentException("JUnitPlugin can only be applied to a project");
        }
    }
}
