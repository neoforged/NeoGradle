package net.neoforged.gradle.common.util.run;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.neoforged.gradle.common.runs.run.RunImpl;
import net.neoforged.gradle.common.runs.tasks.RunExec;
import net.neoforged.gradle.dsl.common.extensions.Minecraft;
import net.neoforged.gradle.dsl.common.extensions.ProjectHolder;
import net.neoforged.gradle.dsl.common.runs.run.Run;
import net.neoforged.gradle.util.StringCapitalizationUtils;
import org.gradle.api.Project;
import org.gradle.api.Transformer;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskProvider;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        
        run.getEnvironmentVariables().put("MOD_CLASSES", buildModClasses(run.getModSources()));
        
        return run;
    }
    
    public static Provider<String> buildModClasses(final ListProperty<SourceSet> sourceSetsProperty) {
        return sourceSetsProperty.map(sourceSets -> {
            final Multimap<String, SourceSet> sourceSetsByProject = HashMultimap.create();
            sourceSets.forEach(sourceSet -> sourceSetsByProject.put(sourceSet.getExtensions().getByType(ProjectHolder.class).getProject().getExtensions().getByType(Minecraft.class).getModIdentifier().get(), sourceSet));
            
            return sourceSetsByProject.entries()
                           .stream().flatMap(entry -> Stream.concat(Stream.of(entry.getValue().getOutput().getResourcesDir()), entry.getValue().getOutput().getClassesDirs().getFiles().stream())
                                          .map(directory -> String.format("%s%%%%%s", entry.getKey(), directory.getAbsolutePath())))
                           .collect(Collectors.joining(File.pathSeparator));
        });
        
        
    }
    
    private static String createTaskName(final String runName) {
        final String conventionTaskName = runName.replaceAll("[^a-zA-Z0-9\\-_]", "");
        if (conventionTaskName.startsWith("run")) {
            return conventionTaskName;
        }
        
        return "run" + StringCapitalizationUtils.capitalize(conventionTaskName);
    }
}
