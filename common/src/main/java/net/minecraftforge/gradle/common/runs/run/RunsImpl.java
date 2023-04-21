package net.minecraftforge.gradle.common.runs.run;

import net.minecraftforge.gradle.common.runs.tasks.RunExec;
import net.minecraftforge.gradle.common.util.DelegatingDomainObjectContainer;
import net.minecraftforge.gradle.dsl.common.runs.run.Run;
import net.minecraftforge.gradle.dsl.common.runs.run.Runs;
import net.minecraftforge.gradle.util.StringCapitalizationUtils;
import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskProvider;

import javax.inject.Inject;

public abstract class RunsImpl extends DelegatingDomainObjectContainer<Run> implements Runs {

    @Inject
    public RunsImpl(Project project) {
        super(project,
                Run.class,
                name -> {
                    final RunImpl run = project.getObjects().newInstance(RunImpl.class, project, name);

                    final TaskProvider<RunExec> runTask = project.getTasks().register(createTaskName(name), RunExec.class, runExec -> {
                        runExec.getRun().set(run);
                    });

                    project.afterEvaluate(evaluatedProject -> runTask.configure(task -> {
                        task.getRun().get().getModSources().get().stream().map(SourceSet::getClassesTaskName)
                                .map(classTaskName -> evaluatedProject.getTasks().named(classTaskName))
                                .forEach(task::dependsOn);
                    }));

                    return run;
                }
        );
    }

    private static String createTaskName(final String runName) {
        final String conventionTaskName = runName.replaceAll("[^a-zA-Z0-9\\-_]", "");
        if (conventionTaskName.startsWith("run")) {
            return conventionTaskName;
        }

        return "run" + StringCapitalizationUtils.capitalize(conventionTaskName);
    }
}
