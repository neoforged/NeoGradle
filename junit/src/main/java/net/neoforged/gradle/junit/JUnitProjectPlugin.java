package net.neoforged.gradle.junit;

import net.neoforged.gradle.common.runs.run.RunImpl;
import net.neoforged.gradle.common.runtime.definition.CommonRuntimeDefinition;
import net.neoforged.gradle.common.util.SourceSetUtils;
import net.neoforged.gradle.common.util.TaskDependencyUtils;
import net.neoforged.gradle.common.util.exceptions.MultipleDefinitionsFoundException;
import net.neoforged.gradle.common.util.exceptions.NoDefinitionsFoundException;
import net.neoforged.gradle.common.util.run.RunsUtil;
import net.neoforged.gradle.dsl.common.extensions.Minecraft;
import net.neoforged.gradle.dsl.common.runs.task.extensions.TestTaskExtension;
import net.neoforged.gradle.userdev.runtime.definition.UserDevRuntimeDefinition;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.dsl.DependencyFactory;
import org.gradle.api.internal.tasks.testing.junitplatform.JUnitPlatformTestFramework;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.testing.Test;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JUnitProjectPlugin implements Plugin<Project> {
    private static final Logger LOG = LoggerFactory.getLogger(JUnitProjectPlugin.class);

    @Override
    public void apply(Project project) {
        project.getTasks().named("test").configure(task -> task.getExtensions().create(TestTaskExtension.NAME, TestTaskExtension.class, project));
        project.afterEvaluate(this::applyAfterEvaluate);
    }

    private void applyAfterEvaluate(Project project) {
        Test testTask = (Test) project.getTasks().getByName("test");
        if (!(testTask.getTestFrameworkProperty().get() instanceof JUnitPlatformTestFramework)) {
            LOG.info("Ignoring test task {} because it doesn't use JUnit 5", testTask.getName());
            return;
        }

        configureTestTask(testTask, "junit");
    }

    /**
     * Configures a JUnit Test-Task to have the required system properties, environment variables, jvm arguments
     * and task dependencies to run the JUnit support code from fml-junit.
     *
     * @param runType The name of the {@link net.neoforged.gradle.dsl.common.runs.type.RunType} to read
     *                the properties from.
     */
    private static void configureTestTask(Test testTask, String runType) {
        TestTaskExtension extension = testTask.getExtensions().getByType(TestTaskExtension.class);
        Project project = testTask.getProject();

        // Find the runtime reachable from the test task
        @Nullable CommonRuntimeDefinition<?> runtimeDefinition;
        try {
            runtimeDefinition = TaskDependencyUtils.extractRuntimeDefinition(project, testTask);
        } catch (MultipleDefinitionsFoundException e) {
            throw new GradleException("Couldn't determine Minecraft runtime definition for JUnit tests", e);
        } catch (NoDefinitionsFoundException e) {
            // Try again to see if it's a forge-dev project
            Object runtimeExtension = project.getExtensions().findByName("runtime");
            if (runtimeExtension instanceof CommonRuntimeDefinition) {
                runtimeDefinition = (CommonRuntimeDefinition<?>) runtimeExtension;
            } else {
                runtimeDefinition = null;
            }
        }

        // If it is a userdev runtime, we automatically add the additional testing libraries declared by the runtime
        // this will add the FML JUnit support library without the user having to declare it themselves.
        Configuration testRuntimeOnly = project.getConfigurations().getByName("testRuntimeOnly");
        DependencyFactory dependencyFactory = project.getDependencyFactory();
        if (runtimeDefinition instanceof UserDevRuntimeDefinition) {
            UserDevRuntimeDefinition userdevRuntime = (UserDevRuntimeDefinition) runtimeDefinition;
            List<String> testDependencies = userdevRuntime.getUserdevConfiguration().getAdditionalTestDependencyArtifactCoordinates().get();
            for (String testDependency : testDependencies) {
                testRuntimeOnly.getDependencies().add(dependencyFactory.create(testDependency));
            }
        }

        RunImpl junitRun = getOrCreateRun("junitTest", project, runtimeDefinition, runType);

        testTask.setWorkingDir(junitRun.getWorkingDirectory().get());
        // Wire up the execution parameters for the Test task with the definitions found in the run
        testTask.getSystemProperties().putAll(junitRun.getSystemProperties().get());
        File argsFile = new File(testTask.getWorkingDir(), "test_args.txt");
        testTask.doFirst("writeArgs", task -> {
            if (!testTask.getWorkingDir().exists()) {
                testTask.getWorkingDir().mkdirs();
            }
            try {
                Files.write(argsFile.toPath(), junitRun.getProgramArguments().get(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        testTask.systemProperty("fml.junit.argsfile", argsFile.getAbsolutePath());
        testTask.getEnvironment().putAll(junitRun.getEnvironmentVariables().get());
        testTask.setJvmArgs(junitRun.getJvmArguments().get());

        // Extend MOD_CLASSES with test sources
        List<String> modClassesDirs = new ArrayList<>();
        final Iterator<SourceSet> sets = Stream.concat(
                extension.getTestSources().get().stream(), junitRun.getModSources().get().stream()
        ).distinct().iterator();
        
        final Set<File> excludedPaths = new HashSet<>();
        while (sets.hasNext()) {
            final SourceSet sourceSet = sets.next();
            final String modId = SourceSetUtils.getModIdentifier(sourceSet);
            Stream.concat(Stream.of(sourceSet.getOutput().getResourcesDir()), sourceSet.getOutput().getClassesDirs().getFiles().stream())
                    .forEach(dir -> {
                        excludedPaths.add(dir);
                        modClassesDirs.add(modId + "%%" + dir.getAbsolutePath());
                    });
        }

        // Make sure that mod classes don't end up being loaded from the boot CP
        testTask.setClasspath(testTask.getClasspath().filter(f -> !excludedPaths.contains(f)));
        testTask.getEnvironment().put("MOD_CLASSES", String.join(File.pathSeparator, modClassesDirs));

        for (TaskProvider<? extends Task> taskDependency : junitRun.getTaskDependencies()) {
            testTask.dependsOn(taskDependency);
        }
    }

    private static RunImpl getOrCreateRun(String runName, Project project, @Nullable CommonRuntimeDefinition<?> runtimeDefinition, String runType) {
        // Reuse an existing junitTest run, otherwise create it
        RunImpl junitRun = (RunImpl) RunsUtil.get(project, runName);
        if (junitRun == null) {
            junitRun = project.getObjects().newInstance(RunImpl.class, runName);
            // Run-Type "junit" must be provided by the runtime or inline in the project
            junitRun.configure(runType);
            junitRun.configure();
            if (runtimeDefinition != null) {
                runtimeDefinition.configureRun(junitRun);
            }
        }
        return junitRun;
    }
}
