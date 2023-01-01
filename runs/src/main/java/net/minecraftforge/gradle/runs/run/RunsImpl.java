package net.minecraftforge.gradle.runs.run;

import net.minecraftforge.gradle.common.util.ConfigurableNamedDSLObjectContainer;
import net.minecraftforge.gradle.common.util.Utils;
import net.minecraftforge.gradle.dsl.runs.run.Run;
import net.minecraftforge.gradle.dsl.runs.run.Runs;
import net.minecraftforge.gradle.runs.tasks.RunExec;
import org.gradle.api.Project;

import javax.inject.Inject;

public abstract class RunsImpl extends ConfigurableNamedDSLObjectContainer<Runs, Run> implements Runs {

    @Inject
    public RunsImpl(Project project) {
        super(project, Run.class, name -> {
            final RunImpl run = project.getObjects().newInstance(RunImpl.class, project, name);

            project.getTasks().register(createTaskName(name), RunExec.class, runExec -> {
                runExec.getRun().set(run);
            });

            return run;
        });
    }

    private static final String createTaskName(final String runName) {
        final String conventionTaskName = runName.replaceAll("[^a-zA-Z0-9\\-_]", "");
        if (conventionTaskName.startsWith("run")) {
            return conventionTaskName;
        }

        return "run" + Utils.capitalize(conventionTaskName);
    }
}
