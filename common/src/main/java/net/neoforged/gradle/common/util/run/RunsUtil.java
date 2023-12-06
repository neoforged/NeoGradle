package net.neoforged.gradle.common.util.run;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.neoforged.gradle.common.runs.run.RunImpl;
import net.neoforged.gradle.common.runs.tasks.RunExec;
import net.neoforged.gradle.common.util.SourceSetUtils;
import net.neoforged.gradle.dsl.common.runs.idea.extensions.IdeaRunsExtension;
import net.neoforged.gradle.dsl.common.runs.run.Run;
import net.neoforged.gradle.util.StringCapitalizationUtils;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.plugins.ide.eclipse.model.EclipseModel;
import org.gradle.plugins.ide.idea.model.IdeaModel;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RunsUtil {
    
    private RunsUtil() {
        throw new IllegalStateException("Tried to create utility class!");
    }
    
    public static String createTaskName(final Run run) {
        return createTaskName(run.getName());
    }
    
    public static String createTaskName(final String prefix, final Run run) {
        return createTaskName(prefix, run.getName());
    }
    
    public static Run create(final Project project, final String name) {
        final RunImpl run = project.getObjects().newInstance(RunImpl.class, project, name);
        
        final TaskProvider<RunExec> runTask = project.getTasks().register(createTaskName(name), RunExec.class, runExec -> {
            runExec.getRun().set(run);
        });
        
        project.afterEvaluate(evaluatedProject -> runTask.configure(task -> {
            addRunSourcesDependenciesToTask(task, run);
            
            run.getTaskDependencies().forEach(task::dependsOn);
        }));
        
        run.getEnvironmentVariables().put("MOD_CLASSES", buildGradleModClasses(run.getModSources()));
        
        return run;
    }

    public static void addRunSourcesDependenciesToTask(Task task, Run run) {
        for (SourceSet sourceSet : run.getModSources().get()) {
            final Project sourceSetProject = SourceSetUtils.getProject(sourceSet);

            //The following tasks are not guaranteed to be in the source sets build dependencies
            //We however need at least the classes as well as the resources of the source set to be run
            task.dependsOn(sourceSetProject.getTasks().named(sourceSet.getProcessResourcesTaskName()));
            task.dependsOn(sourceSetProject.getTasks().named(sourceSet.getCompileJavaTaskName()));

            //There might be additional tasks that are needed to configure and run a source set.
            //Also run those
            sourceSet.getOutput().getBuildDependencies().getDependencies(null)
                    .forEach(task::dependsOn);
        }
    }
    
    public static Provider<String> buildGradleModClasses(final ListProperty<SourceSet> sourceSetsProperty) {
        return buildGradleModClasses(
                sourceSetsProperty,
                sourceSet -> Stream.concat(Stream.of(sourceSet.getOutput().getResourcesDir()), sourceSet.getOutput().getClassesDirs().getFiles().stream())
        );
    }
    
    public static Provider<String> buildRunWithIdeaModClasses(final ListProperty<SourceSet> sourceSetsProperty) {
        return buildGradleModClasses(
                sourceSetsProperty,
                sourceSet -> {
                    final Project project = SourceSetUtils.getProject(sourceSet);
                    final IdeaModel rootIdeaModel = project.getRootProject().getExtensions().getByType(IdeaModel.class);
                    final IdeaRunsExtension ideaRunsExtension = ((ExtensionAware) rootIdeaModel
                                                                  .getProject())
                                                                  .getExtensions().getByType(IdeaRunsExtension.class);
                    
                    if (ideaRunsExtension.getRunWithIdea().get()) {
                        final File parentDir = ideaRunsExtension.getOutDirectory().get().getAsFile();
                        final File sourceSetDir = new File(parentDir, getIntellijOutName(sourceSet));
                        return Stream.of(new File(sourceSetDir, "resources"), new File(sourceSetDir, "classes"));
                    }
                    
                    return Stream.concat(Stream.of(sourceSet.getOutput().getResourcesDir()), sourceSet.getOutput().getClassesDirs().getFiles().stream());
                }
        );
    }
    
    @SuppressWarnings("UnstableApiUsage")
    public static Provider<String> buildRunWithEclipseModClasses(final ListProperty<SourceSet> sourceSetsProperty) {
        return buildGradleModClasses(
                sourceSetsProperty,
                sourceSet -> {
                    final Project project = SourceSetUtils.getProject(sourceSet);
                    final EclipseModel eclipseModel = project.getExtensions().getByType(EclipseModel.class);
                    
                    final File conventionsDir = new File(project.getProjectDir(), "bin");
                    eclipseModel.getClasspath().getBaseSourceOutputDir().convention(project.provider(() -> conventionsDir));
                    
                    final File parentDir = eclipseModel.getClasspath().getBaseSourceOutputDir().get();
                    final File sourceSetDir = new File(parentDir, sourceSet.getName());
                    return Stream.of(sourceSetDir);
                }
        );
    }
    
    public static String getIntellijOutName(@Nonnull final SourceSet sourceSet) {
        return sourceSet.getName().equals(SourceSet.MAIN_SOURCE_SET_NAME) ? "production" : sourceSet.getName();
    }
    
    public static Provider<String> buildGradleModClasses(final ListProperty<SourceSet> sourceSetsProperty, final Function<SourceSet, Stream<File>> directoryBuilder) {
        return sourceSetsProperty.map(sourceSets -> {
            final Multimap<String, SourceSet> sourceSetsByRunId = HashMultimap.create();
            sourceSets.forEach(sourceSet -> sourceSetsByRunId.put(SourceSetUtils.getModIdentifier(sourceSet), sourceSet));
            
            return sourceSetsByRunId.entries()
                           .stream().flatMap(entry -> directoryBuilder.apply(entry.getValue())
                                                              .map(directory -> String.format("%s%%%%%s", entry.getKey(), directory.getAbsolutePath())))
                           .collect(Collectors.joining(File.pathSeparator));
        });
    }
    
    private static String createTaskName(final String runName) {
        return createTaskName("run", runName);
    }
    
    private static String createTaskName(final String prefix, final String runName) {
        final String conventionTaskName = runName.replaceAll("[^a-zA-Z0-9\\-_]", "");
        if (conventionTaskName.startsWith("run")) {
            return conventionTaskName;
        }
        
        return prefix + StringCapitalizationUtils.capitalize(conventionTaskName);
    }
}
