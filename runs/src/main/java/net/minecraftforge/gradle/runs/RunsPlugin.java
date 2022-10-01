package net.minecraftforge.gradle.runs;

import net.minecraftforge.gradle.runs.config.RunConfigurationSpec;
import net.minecraftforge.gradle.runs.util.RunsConstants;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class RunsPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.getExtensions().add(RunsConstants.Extensions.RUNS, project.getObjects().domainObjectContainer(RunConfiguration.class, name -> project.getObjects().newInstance(RunConfiguration.class, project, name)));
        project.getExtensions().add(RunsConstants.Extensions.RUN_SPECS, project.getObjects().domainObjectContainer(RunConfigurationSpec.class, name -> project.getObjects().newInstance(RunConfigurationSpec.class, project, name)));
    }
}
