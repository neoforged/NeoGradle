package net.neoforged.gradle.common.util.run;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import net.neoforged.gradle.common.runs.run.RunImpl;
import net.neoforged.gradle.common.runs.tasks.RunExec;
import net.neoforged.gradle.common.util.SourceSetUtils;
import net.neoforged.gradle.dsl.common.runs.idea.extensions.IdeaRunsExtension;
import net.neoforged.gradle.dsl.common.runs.run.DependencyHandler;
import net.neoforged.gradle.dsl.common.runs.run.Run;
import net.neoforged.gradle.dsl.common.runs.run.RunDependency;
import net.neoforged.gradle.util.StringCapitalizationUtils;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.plugins.jvm.JvmTestSuite;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.testing.Test;
import org.gradle.plugins.ide.eclipse.model.EclipseModel;
import org.gradle.plugins.ide.idea.model.IdeaModel;
import org.gradle.process.CommandLineArgumentProvider;
import org.gradle.testing.base.TestingExtension;

import javax.annotation.Nonnull;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

        project.afterEvaluate(evaluatedProject -> {
            // Make sure that the sourceset associated with that test suite is added to the env var
            if (run.getIsJUnit().get()) {
                run.getModSources().add(project.getExtensions().getByType(JavaPluginExtension.class)
                        .getSourceSets().named(name));
            }
            run.getEnvironmentVariables().put("MOD_CLASSES", buildGradleModClasses(run.getModSources()));

            if (run.getIsJUnit().get()) {
                project.getExtensions().configure(TestingExtension.class, ext -> {
                    JvmTestSuite suite = (JvmTestSuite) ext.getSuites().findByName(name);

                    if (suite == null) {
                        suite = ext.getSuites().create(name, JvmTestSuite.class);
                    }

                    final DependencyHandler deps = run.getDependencies().get();

                    deps.runtime(deps.configuration(project.getConfigurations().getByName(name + "BootImplementation")));

                    suite.useJUnitJupiter();
                    suite.getTargets().configureEach(target -> target.getTestTask().configure(test -> {
                        test.useJUnitPlatform();
                        test.workingDir(run.getWorkingDirectory().get());
                        test.environment(run.getEnvironmentVariables().get());

                        final File jvmArgs = new File(test.getWorkingDir(), "jvmargs.txt");
                        final File mainArgs = new File(test.getWorkingDir(), "mainargs.txt");
                        test.doFirst(t -> {
                            if (!jvmArgs.exists()) {
                                jvmArgs.getParentFile().mkdirs();
                            }

                            // Remove the module path, we're adding it somewhere else
                            final List<String> jargs = new ArrayList<>(run.getJvmArguments().get());
                            if (jargs.contains("-p")) {
                                jargs.remove(jargs.indexOf("-p") + 1);
                                jargs.remove("-p");
                            }

                            try {
                                Files.write(mainArgs.toPath(), run.getProgramArguments().get());
                                Files.write(jvmArgs.toPath(), jargs);
                            } catch (Exception ex) {
                                throw new RuntimeException(ex);
                            }
                        });

                        // The other "boot" dependencies will also have to live on the module path explicitly and be added to the ignore list
                        final List<File> otherDeps = deps.getRuntime().get().stream()
                                .flatMap(dep -> dep.getDependency().getFiles().stream())
                                .collect(Collectors.toList());

                        final Map<String, String> sysProps = new HashMap<>(run.getSystemProperties().get());
                        sysProps.put("ignoreList", sysProps.get("ignoreList") + ","
                            + otherDeps.stream().map(File::getName).collect(Collectors.joining(",")));
                        test.systemProperties(sysProps);
                        test.systemProperty("fml.junit.argsfile", mainArgs.getAbsolutePath());
                        test.jvmArgs("@jvmargs.txt");

                        test.getJvmArgumentProviders().add(() -> Lists.newArrayList("--module-path", Stream.concat(
                                // Grab the CP from the JVM args
                                Arrays.stream(run.getJvmArguments().get().get(run.getJvmArguments().get().indexOf("-p") + 1).split(File.pathSeparator)),
                                otherDeps.stream().map(File::getAbsolutePath)
                        ).collect(Collectors.joining(File.pathSeparator))));

                        run.getTaskDependencies().forEach(test::dependsOn);
                    }));
                });
            }

            runTask.configure(task -> {
                addRunSourcesDependenciesToTask(task, run);

                run.getTaskDependencies().forEach(task::dependsOn);
                task.setEnabled(!run.getIsJUnit().get());
            });
        });
        
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
