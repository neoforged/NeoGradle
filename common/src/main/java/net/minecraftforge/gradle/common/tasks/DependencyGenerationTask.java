package net.minecraftforge.gradle.common.tasks;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

@DisableCachingByDefault(because = "This task does nothing and needs to be triggered to run dependency resolution.")
public class DependencyGenerationTask extends DefaultTask {


    @TaskAction
    public void doTask() {
        getLogger().debug("Provided dependencies to: " + getProject().getPath());
    }
}
