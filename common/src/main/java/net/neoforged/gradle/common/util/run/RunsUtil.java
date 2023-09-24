package net.neoforged.gradle.common.util.run;

import net.neoforged.gradle.common.runs.run.RunImpl;
import net.neoforged.gradle.common.runs.tasks.RunExec;
import net.neoforged.gradle.dsl.common.runs.run.Run;
import net.neoforged.gradle.util.StringCapitalizationUtils;
import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskProvider;

public class RunsUtil {
    
    private RunsUtil() {
        throw new IllegalStateException("Tried to create utility class!");
    }
    
    public static Run create(final Project project, final String name) {
        final RunImpl run = project.getObjects().newInstance(RunImpl.class, project, name);
        
        final TaskProvider<RunExec> runTask = project.getTasks().register(createTaskName(name), RunExec.class, runExec -> {
            runExec.getRun().set(run);
        });
        
        project.afterEvaluate(evaluatedProject -> runTask.configure(task -> {
            task.getRun().get().getModSources().get().stream().map(SourceSet::getClassesTaskName)
                    .map(classTaskName -> evaluatedProject.getTasks().named(classTaskName))
                    .forEach(task::dependsOn);
            
            run.getTaskDependencies().forEach(task::dependsOn);
        }));
        
        return run;
    }
    
    private static String createTaskName(final String runName) {
        final String conventionTaskName = runName.replaceAll("[^a-zA-Z0-9\\-_]", "");
        if (conventionTaskName.startsWith("run")) {
            return conventionTaskName;
        }
        
        return "run" + StringCapitalizationUtils.capitalize(conventionTaskName);
    }
}
