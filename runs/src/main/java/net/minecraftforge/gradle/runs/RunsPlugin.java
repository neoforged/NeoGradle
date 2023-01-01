package net.minecraftforge.gradle.runs;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class RunsPlugin implements Plugin<Object> {
    @Override
    public void apply(Object target) {
        if (target instanceof Project) {
            Project project = (Project) target;
            project.getPluginManager().apply(RunsProjectPlugin.class);
        }
        else {
            throw new IllegalArgumentException("RunsPlugin can only be applied to a Project");
        }
    }
}
