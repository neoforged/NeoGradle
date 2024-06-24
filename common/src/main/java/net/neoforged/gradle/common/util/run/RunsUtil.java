package net.neoforged.gradle.common.util.run;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.neoforged.gradle.common.runs.run.RunImpl;
import net.neoforged.gradle.common.util.ClasspathUtils;
import net.neoforged.gradle.common.util.SourceSetUtils;
import net.neoforged.gradle.dsl.common.extensions.subsystems.Subsystems;
import net.neoforged.gradle.dsl.common.extensions.subsystems.conventions.Runs;
import net.neoforged.gradle.dsl.common.runs.idea.extensions.IdeaRunsExtension;
import net.neoforged.gradle.dsl.common.runs.run.Run;
import net.neoforged.gradle.util.StringCapitalizationUtils;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.testing.Test;
import org.gradle.jvm.toolchain.JavaToolchainService;
import org.gradle.plugins.ide.eclipse.model.EclipseModel;
import org.gradle.plugins.ide.idea.model.IdeaModel;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Set;
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

        //Configure mod sources env vars
        project.afterEvaluate(evaluatedProject -> {
            //Create a combined provider for the mod and unit test sources
            Provider<Multimap<String, SourceSet>> sourceSets = run.getModSources().all().zip(
                    run.getUnitTestSources().all(),
                    (modSources, unitTestSources) -> {
                        if (!run.getIsJUnit().get())
                            //No Unit test sources for non-unit test runs
                            return modSources;

                        //Combine mod sources with unit test sources
                        final HashMultimap<String, SourceSet> combined = HashMultimap.create(modSources);
                        combined.putAll(unitTestSources);
                        return combined;
                    });
            //Set the mod classes environment variable
            run.getEnvironmentVariables().put("MOD_CLASSES", buildGradleModClasses(sourceSets));
        });

        project.afterEvaluate(evaluatedProject -> {
            if (!run.getIsJUnit().get()) {
                //Create run exec tasks for all non-unit test runs
                project.getTasks().register(createTaskName(name), JavaExec.class, runExec -> {
                    runExec.setDescription("Runs the " + name + " run.");
                    runExec.setGroup("NeoGradle/Runs");

                    JavaToolchainService service = evaluatedProject.getExtensions().getByType(JavaToolchainService.class);
                    runExec.getJavaLauncher().convention(service.launcherFor(evaluatedProject.getExtensions().getByType(JavaPluginExtension.class).getToolchain()));

                    final File workingDir = run.getWorkingDirectory().get().getAsFile();
                    if (!workingDir.exists()) {
                        workingDir.mkdirs();
                    }

                    runExec.getMainClass().convention(run.getMainClass());
                    runExec.setWorkingDir(workingDir);
                    runExec.args(run.getProgramArguments().get());
                    runExec.jvmArgs(run.getJvmArguments().get());
                    runExec.systemProperties(run.getSystemProperties().get());
                    runExec.environment(run.getEnvironmentVariables().get());
                    run.getModSources().all().get().values().stream()
                            .map(SourceSet::getRuntimeClasspath)
                            .forEach(runExec::classpath);
                    runExec.classpath(run.getDependencies().get().getRuntimeConfiguration());
                    runExec.classpath(run.getClasspath());

                    updateRunExecClasspathBasedOnPrimaryTask(runExec, run);

                    addRunSourcesDependenciesToTask(runExec, run);

                    run.getTaskDependencies().forEach(runExec::dependsOn);
                });
            } else {
                createOrReuseTestTask(project, name, run);
            }
        });

        return run;
    }

    private static void updateRunExecClasspathBasedOnPrimaryTask(final JavaExec runExec, final Run run) {
        if (run.getModSources().getPrimary().isPresent()) {
            final SourceSet primary = run.getModSources().getPrimary().get();

            final boolean primaryHasMinecraft = primary.getRuntimeClasspath().getFiles().stream().anyMatch(ClasspathUtils::isMinecraftClasspathEntry);

            //Remove any classpath entries that are already in the primary runtime classpath.
            //Also remove any classpath entries that are Minecraft, we can only have one Minecraft jar, in the case that the primary runtime classpath already has Minecraft.
            final FileCollection runtimeClasspathWithoutMinecraftAndWithoutPrimaryRuntimeClasspath =
                    runExec.classpath().getClasspath().filter(file -> !primary.getRuntimeClasspath().contains(file) && (!primaryHasMinecraft || !ClasspathUtils.isMinecraftClasspathEntry(file)));

            //Combine with the primary runtime classpath.
            final FileCollection combinedClasspath = primary.getRuntimeClasspath().plus(runtimeClasspathWithoutMinecraftAndWithoutPrimaryRuntimeClasspath);
            if (runExec.getClasspath() instanceof ConfigurableFileCollection classpath) {
                //Override
                classpath.setFrom(combinedClasspath);
            } else {
                throw new IllegalStateException("Classpath is not a ConfigurableFileCollection");
            }
        }
    }

    private static void createOrReuseTestTask(Project project, String name, RunImpl run) {
        final Set<SourceSet> currentProjectsModSources = run.getModSources().all().get().values()
                .stream()
                .filter(sourceSet -> SourceSetUtils.getProject(sourceSet).equals(project))
                .collect(Collectors.toSet());

        final Set<SourceSet> currentProjectsTestSources = run.getUnitTestSources().all().get().values()
                .stream()
                .filter(sourceSet -> SourceSetUtils.getProject(sourceSet).equals(project))
                .collect(Collectors.toSet());

        //If the run has only one mod source of this project, and one test source of this project, and these are the main and test sourcesets respectively,
        //we can reuse the test task, if it is allowed.
        if (
                (currentProjectsModSources.size() == 1 && currentProjectsModSources.contains(project.getExtensions().getByType(SourceSetContainer.class).getByName("main")))
                        &&
                        (currentProjectsTestSources.size() == 1 && currentProjectsTestSources.contains(project.getExtensions().getByType(SourceSetContainer.class).getByName("test")))
        ) {
            final Runs runsConventions = project.getExtensions().getByType(Subsystems.class).getConventions().getRuns();
            if (runsConventions.getShouldDefaultTestTaskBeReused().get()) {
                //Get the default test task
                final TaskProvider<Test> testTask = project.getTasks().named("test", Test.class);
                configureTestTask(project, testTask, run);
                return;
            }
        }

        createNewTestTask(project, name, run);
    }

    private static void createNewTestTask(Project project, String name, RunImpl run) {
        //Create a test task for unit tests
        TaskProvider<Test> newTestTask = project.getTasks().register(createTaskName("test", name), Test.class);
        configureTestTask(project, newTestTask, run);
        project.getTasks().named("check", check -> check.dependsOn(newTestTask));
    }

    private static void configureTestTask(Project project, TaskProvider<Test> testTaskProvider, RunImpl run) {
        testTaskProvider.configure(testTask -> {
            addRunSourcesDependenciesToTask(testTask, run);
            run.getTaskDependencies().forEach(testTask::dependsOn);

            testTask.setWorkingDir(run.getWorkingDirectory().get());
            testTask.getSystemProperties().putAll(run.getSystemProperties().get());

            testTask.useJUnitPlatform();

            testTask.setGroup("verification");

            File argsFile = new File(testTask.getWorkingDir(), "test_args.txt");
            final ListProperty<String> programArguments = run.getProgramArguments();
            testTask.doFirst("writeArgs", task -> {
                if (!testTask.getWorkingDir().exists()) {
                    testTask.getWorkingDir().mkdirs();
                }
                try {
                    Files.write(argsFile.toPath(), programArguments.get(), StandardCharsets.UTF_8);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            testTask.systemProperty("fml.junit.argsfile", argsFile.getAbsolutePath());

            testTask.getEnvironment().putAll(run.getEnvironmentVariables().get());
            testTask.setJvmArgs(run.getJvmArguments().get());

            final ConfigurableFileCollection testCP = project.files();
            testCP.from(run.getDependencies().get().getRuntimeConfiguration());
            Stream.concat(run.getModSources().all().get().values().stream(), run.getUnitTestSources().all().get().values().stream())
                    .forEach(src -> testCP.from(filterOutput(src)));

            testTask.setClasspath(testCP);

            final ConfigurableFileCollection testClassesDirs = project.files();
            for (SourceSet sourceSet : run.getUnitTestSources().all().get().values()) {
                testClassesDirs.from(sourceSet.getOutput().getClassesDirs());
            }

            testTask.setTestClassesDirs(testClassesDirs);
        });
    }

    private static FileCollection filterOutput(SourceSet srcSet) {
        FileCollection collection = srcSet.getRuntimeClasspath();
        if (srcSet.getOutput().getResourcesDir() != null) {
            final File resourcesDir = srcSet.getOutput().getResourcesDir();
            collection = collection.filter(file -> !file.equals(resourcesDir));
        }

        FileCollection classesDirs = srcSet.getOutput().getClassesDirs();
        collection = collection.filter(file -> !classesDirs.contains(file));
        return collection;
    }

    public static void addRunSourcesDependenciesToTask(Task task, Run run) {
        for (SourceSet sourceSet : run.getModSources().all().get().values()) {
            final Project sourceSetProject = SourceSetUtils.getProject(sourceSet);

            //The following tasks are not guaranteed to be in the source sets build dependencies
            //We however need at least the classes as well as the resources of the source set to be run
            task.dependsOn(sourceSetProject.getTasks().named(sourceSet.getProcessResourcesTaskName()));
            task.dependsOn(sourceSetProject.getTasks().named(sourceSet.getCompileJavaTaskName()));

            //There might be additional tasks that are needed to configure and run a source set.
            //Also run those
            sourceSet.getOutput().getBuildDependencies().getDependencies(null).forEach(task::dependsOn);
        }
    }

    public static Provider<String> buildGradleModClasses(final Provider<Multimap<String, SourceSet>> sourceSetsProperty) {
        return buildGradleModClasses(sourceSetsProperty, sourceSet -> Stream.concat(Stream.of(sourceSet.getOutput().getResourcesDir()), sourceSet.getOutput().getClassesDirs().getFiles().stream()));
    }

    public static Provider<String> buildRunWithIdeaModClasses(final Provider<Multimap<String, SourceSet>> sourceSetsProperty) {
        return buildGradleModClasses(sourceSetsProperty, sourceSet -> {
            final Project project = SourceSetUtils.getProject(sourceSet);
            final IdeaModel rootIdeaModel = project.getRootProject().getExtensions().getByType(IdeaModel.class);
            final IdeaRunsExtension ideaRunsExtension = ((ExtensionAware) rootIdeaModel.getProject()).getExtensions().getByType(IdeaRunsExtension.class);

            if (ideaRunsExtension.getRunWithIdea().get()) {
                final File parentDir = ideaRunsExtension.getOutDirectory().get().getAsFile();
                final File sourceSetDir = new File(parentDir, getIntellijOutName(sourceSet));
                return Stream.of(new File(sourceSetDir, "resources"), new File(sourceSetDir, "classes"));
            }

            return Stream.concat(Stream.of(sourceSet.getOutput().getResourcesDir()), sourceSet.getOutput().getClassesDirs().getFiles().stream());
        });
    }

    @SuppressWarnings("UnstableApiUsage")
    public static Provider<String> buildRunWithEclipseModClasses(final Provider<Multimap<String, SourceSet>> sourceSetsProperty) {
        return buildGradleModClasses(sourceSetsProperty, sourceSet -> {
            final Project project = SourceSetUtils.getProject(sourceSet);
            final EclipseModel eclipseModel = project.getExtensions().getByType(EclipseModel.class);

            final File conventionsDir = new File(project.getProjectDir(), "bin");
            eclipseModel.getClasspath().getBaseSourceOutputDir().convention(project.provider(() -> conventionsDir));

            final File parentDir = eclipseModel.getClasspath().getBaseSourceOutputDir().get();
            final File sourceSetDir = new File(parentDir, sourceSet.getName());
            return Stream.of(sourceSetDir);
        });
    }

    public static String getIntellijOutName(@Nonnull final SourceSet sourceSet) {
        return sourceSet.getName().equals(SourceSet.MAIN_SOURCE_SET_NAME) ? "production" : sourceSet.getName();
    }

    public static Provider<String> buildGradleModClasses(final Provider<Multimap<String, SourceSet>> sourceSetsProperty, final Function<SourceSet, Stream<File>> directoryBuilder) {
        return sourceSetsProperty.map(sourceSetsByRunId -> sourceSetsByRunId.entries().stream().flatMap(entry ->
                        directoryBuilder.apply(entry.getValue())
                                .peek(directory -> directory.mkdirs())
                                .map(directory -> String.format("%s%%%%%s", entry.getKey(), directory.getAbsolutePath())))
                .collect(Collectors.joining(File.pathSeparator)));
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
