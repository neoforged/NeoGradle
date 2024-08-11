package net.neoforged.gradle.common.util.run;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.neoforged.gradle.common.extensions.IdeManagementExtension;
import net.neoforged.gradle.common.extensions.NeoGradleProblemReporter;
import net.neoforged.gradle.common.runs.run.RunImpl;
import net.neoforged.gradle.common.tasks.RenderDocDownloaderTask;
import net.neoforged.gradle.common.util.ClasspathUtils;
import net.neoforged.gradle.common.util.ConfigurationUtils;
import net.neoforged.gradle.common.util.SourceSetUtils;
import net.neoforged.gradle.common.util.VersionJson;
import net.neoforged.gradle.dsl.common.extensions.subsystems.*;
import net.neoforged.gradle.dsl.common.extensions.subsystems.conventions.Runs;
import net.neoforged.gradle.dsl.common.extensions.subsystems.tools.RenderDocTools;
import net.neoforged.gradle.dsl.common.runs.idea.extensions.IdeaRunsExtension;
import net.neoforged.gradle.dsl.common.runs.run.Run;
import net.neoforged.gradle.dsl.common.runs.run.RunDevLoginOptions;
import net.neoforged.gradle.util.StringCapitalizationUtils;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.*;
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
import org.gradle.plugins.ide.idea.model.IdeaModule;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.xml.sax.InputSource;

import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RunsUtil {

    private RunsUtil() {
        throw new IllegalStateException("Tried to create utility class!");
    }

    public static String createTaskName(final String prefix, final Run run) {
        return createTaskName(prefix, run.getName());
    }

    public static void configure(Project project, Run run, boolean isInternal) {
        RunsUtil.configureModSourceDefaults(project, run);

        run.configure();

        RunsUtil.setupModSources(project, run, isInternal);
        RunsUtil.configureModClasses(run);
        RunsUtil.ensureMacOsSupport(run);
        RunsUtil.setupDevLoginSupport(project, run);
        RunsUtil.setupRenderDocSupport(project, run);
        if (!isInternal) {
            RunsUtil.createTasks(project, run);
        }
        RunsUtil.registerPostSyncTasks(project, run);
    }

    public static void registerPostSyncTasks(Project project, Run run) {
        final IdeManagementExtension ideManager = project.getExtensions().getByType(IdeManagementExtension.class);
        run.getPostSyncTasks().get().forEach(ideManager::registerTaskToRun);
    }

    public static void createTasks(Project project, Run run) {
        if (!run.getIsJUnit().get()) {
            //Create run exec tasks for all non-unit test runs
            project.getTasks().register(createTaskName(run.getName()), JavaExec.class, runExec -> {
                runExec.setDescription("Runs the " + run.getName() + " run.");
                runExec.setGroup("NeoGradle/Runs");

                JavaToolchainService service = project.getExtensions().getByType(JavaToolchainService.class);
                runExec.getJavaLauncher().convention(service.launcherFor(project.getExtensions().getByType(JavaPluginExtension.class).getToolchain()));

                final File workingDir = run.getWorkingDirectory().get().getAsFile();
                if (!workingDir.exists()) {
                    workingDir.mkdirs();
                }

                runExec.getMainClass().convention(run.getMainClass());
                runExec.setWorkingDir(workingDir);
                runExec.args(deduplicateElementsFollowingEachOther(run.getArguments().get().stream()).toList());
                runExec.jvmArgs(deduplicateElementsFollowingEachOther(run.getJvmArguments().get().stream()).toList());
                runExec.systemProperties(run.getSystemProperties().get());
                runExec.environment(run.getEnvironmentVariables().get());
                run.getModSources().all().get().values().stream()
                        .map(SourceSet::getRuntimeClasspath)
                        .forEach(runExec::classpath);
                runExec.classpath(run.getDependencies().getRuntimeConfiguration());
                runExec.classpath(run.getRuntimeClasspath());

                updateRunExecClasspathBasedOnPrimaryTask(runExec, run);

                addRunSourcesDependenciesToTask(runExec, run, true);

                runExec.getDependsOn().add(run.getDependsOn());
                runExec.getDependsOn().add(run.getPostSyncTasks()); //We need this additionally here, in-case the user runs this through gradle without an IDE
            });
        } else {
            createOrReuseTestTask(project, run.getName(), run);
        }
    }

    public static void ensureMacOsSupport(Run run) {
        //When we are on mac-os we need to add the -XstartOnFirstThread argument to the JVM arguments
        if (VersionJson.OS.getCurrent() == VersionJson.OS.OSX && run.getIsClient().get()) {
            //This argument is only needed on the client.
            run.getJvmArguments().add("-XstartOnFirstThread");
        }
    }

    public static void configureModSourceDefaults(Project project, Run run) {
        final Conventions conventions = project.getExtensions().getByType(Subsystems.class).getConventions();
        if (conventions.getSourceSets().getShouldMainSourceSetBeAutomaticallyAddedToRuns().get()) {
            //We always register main
            run.getModSources().add(project.getExtensions().getByType(SourceSetContainer.class).getByName("main"));
        }
    }

    public static void setupModSources(Project project, Run run, boolean isInternal) {
        // We add default junit sourcesets here because we need to know the type of the run first
        final Conventions conventions = project.getExtensions().getByType(Subsystems.class).getConventions();
        if (!isInternal && conventions.getSourceSets().getShouldTestSourceSetBeAutomaticallyAddedToRuns().get()) {
            if (run.getIsJUnit().get()) {
                run.getUnitTestSources().add(project.getExtensions().getByType(SourceSetContainer.class).getByName("test"));
            }
        }

        if (conventions.getSourceSets().getShouldSourceSetsLocalRunRuntimesBeAutomaticallyAddedToRuns().get() && conventions.getConfigurations().getIsEnabled().get()) {
            run.getModSources().all().get().values().forEach(sourceSet -> {
                if (project.getConfigurations().findByName(ConfigurationUtils.getSourceSetName(sourceSet, conventions.getConfigurations().getRunRuntimeConfigurationPostFix().get())) != null) {
                    run.getDependencies().getRuntime().add(project.getConfigurations().getByName(ConfigurationUtils.getSourceSetName(sourceSet, conventions.getConfigurations().getRunRuntimeConfigurationPostFix().get())));
                }
            });
        }

        //Warn the user if no source sets are configured
        if (run.getModSources().all().get().isEmpty()) {
            final NeoGradleProblemReporter reporter = project.getExtensions().getByType(NeoGradleProblemReporter.class);

            throw reporter.throwing(problemSpec -> problemSpec.id("runs", "noSourceSetsConfigured")
                    .contextualLabel("Run: " + run.getName())
                    .details("The run: " + run.getName() + " has no source sets configured")
                    .solution("Please configure at least one source set")
                    .section("handling-of-none-neogradle-sibling-projects"));
        }
    }

    public static void setupDevLoginSupport(Project project, Run run) {
        //Handle dev login.
        final DevLogin devLogin = project.getExtensions().getByType(Subsystems.class).getDevLogin();
        final Tools tools = project.getExtensions().getByType(Subsystems.class).getTools();
        final RunDevLoginOptions runsDevLogin = run.getDevLogin();

        //Dev login is only supported on the client side
        if (run.getIsClient().get() && runsDevLogin.getIsEnabled().get()) {
            final String mainClass = run.getMainClass().get();

            //We add the dev login tool to a custom configuration which runtime classpath extends from the default runtime classpath
            final SourceSet defaultSourceSet = run.getModSources().all().get().entries().iterator().next().getValue();
            final String runtimeOnlyDevLoginConfigurationName = ConfigurationUtils.getSourceSetName(defaultSourceSet, devLogin.getConfigurationSuffix().get());
            final Configuration sourceSetRuntimeOnlyDevLoginConfiguration = project.getConfigurations().maybeCreate(runtimeOnlyDevLoginConfigurationName);
            final Configuration sourceSetRuntimeClasspathConfiguration = project.getConfigurations().maybeCreate(defaultSourceSet.getRuntimeClasspathConfigurationName());

            sourceSetRuntimeClasspathConfiguration.extendsFrom(sourceSetRuntimeOnlyDevLoginConfiguration);
            sourceSetRuntimeOnlyDevLoginConfiguration.getDependencies().add(project.getDependencies().create(tools.getDevLogin().get()));

            //Update the program arguments to properly launch the dev login tool
            run.getArguments().add("--launch_target");
            run.getArguments().add(mainClass);

            if (runsDevLogin.getProfile().isPresent()) {
                run.getArguments().add("--launch_profile");
                run.getArguments().add(runsDevLogin.getProfile().get());
            }

            //Set the main class to the dev login tool
            run.getMainClass().set(devLogin.getMainClass());
        } else if (!run.getIsClient().get() && runsDevLogin.getIsEnabled().get()) {
            final NeoGradleProblemReporter reporter = project.getExtensions().getByType(NeoGradleProblemReporter.class);
            throw reporter.throwing(spec -> spec
                    .id("runs", "dev-login-not-supported")
                    .contextualLabel("Run: " + run.getName())
                    .details("Dev login is only supported on runs which are marked as clients! The run: " + run.getName() + " is not a client run.")
                    .solution("Please mark the run as a client run or disable dev login.")
                    .section("common-runs-devlogin-configuration")
            );
        }
    }

    public static void setupRenderDocSupport(Project project, Run run) {
        if (run.getRenderDoc().getEnabled().get()) {
            if (!run.getIsClient().get())
                throw new InvalidUserDataException("RenderDoc can only be enabled for client runs.");

            final RenderDocTools renderDocTools = project.getExtensions().getByType(Subsystems.class).getTools().getRenderDoc();
            final TaskProvider<RenderDocDownloaderTask> setupRenderDoc = project.getTasks().register(RunsUtil.createTaskName("setupRenderDoc", run), RenderDocDownloaderTask.class, renderDoc -> {
                renderDoc.getRenderDocVersion().set(renderDocTools.getRenderDocVersion());
                renderDoc.getRenderDocOutputDirectory().set(renderDocTools.getRenderDocPath().dir("download"));
                renderDoc.getRenderDocInstallationDirectory().set(renderDocTools.getRenderDocPath().dir("installation"));
            });

            run.getDependsOn().add(setupRenderDoc);

            Configuration renderNurse = null;
            if (run.getModSources().getPrimary().isPresent()) {
                renderNurse = addLocalRenderNurse(run.getModSources().getPrimary().get(), run);
            }

            if (renderNurse == null) {
                //This happens when no primary source set is set, and the renderNurse configuration is not added to the runtime classpath.
                renderNurse = registerRenderNurse(run.getProject());
            }

            //Add the relevant properties, so that render nurse can be used, see its readme for the required values.
            run.getEnvironmentVariables().put("LD_PRELOAD", setupRenderDoc.flatMap(RenderDocDownloaderTask::getRenderDocLibraryFile).map(RegularFile::getAsFile).map(File::getAbsolutePath));
            run.getSystemProperties().put(
                    "neoforge.rendernurse.renderdoc.library", setupRenderDoc.flatMap(RenderDocDownloaderTask::getRenderDocLibraryFile).map(RegularFile::getAsFile).map(File::getAbsolutePath)
            );
            run.getJvmArguments().add(renderNurse.getIncoming().getArtifacts().getResolvedArtifacts()
                    .map(artifactView -> artifactView.iterator().next())
                    .map(resolvedArtifact -> "-javaagent:%s".formatted(resolvedArtifact.getFile().getAbsolutePath())));
            run.getJvmArguments().add("--enable-preview");
            run.getJvmArguments().add("--enable-native-access=ALL-UNNAMED");
        }
    }

    private static Configuration addLocalRenderNurse(SourceSet sourceSet, Run run) {
        final Project project = SourceSetUtils.getProject(sourceSet);
        final Configuration renderNurse = registerRenderNurse(project);

        final Configuration runtimeClasspath = project.getConfigurations().getByName(sourceSet.getRuntimeClasspathConfigurationName());
        runtimeClasspath.extendsFrom(renderNurse);
        return renderNurse;
    }

    private static @NotNull Configuration registerRenderNurse(Project project) {
        final RenderDocTools renderDocTools = project.getExtensions().getByType(Subsystems.class).getTools().getRenderDoc();
        final RenderDoc renderDocSubSystem = project.getExtensions().getByType(Subsystems.class).getRenderDoc();

        return ConfigurationUtils.temporaryUnhandledConfiguration(
                project.getConfigurations(),
                renderDocSubSystem.getConfigurationSuffix().get(),
                project.getDependencies().create(renderDocTools.getRenderNurse().get())
        );
    }

    public static void configureModClasses(Run run) {
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
    }

    public static Run create(final Project project, final String name) {
        return project.getObjects().newInstance(RunImpl.class, project, name);
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

    private static void createOrReuseTestTask(Project project, String name, Run run) {
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

    private static void createNewTestTask(Project project, String name, Run run) {
        //Create a test task for unit tests
        TaskProvider<Test> newTestTask = project.getTasks().register(createTaskName("test", name), Test.class);
        configureTestTask(project, newTestTask, run);
        project.getTasks().named("check", check -> check.dependsOn(newTestTask));
    }

    public static String escapeAndJoin(List<String> args, String... additional) {
        return escapeStream(args, additional).collect(Collectors.joining(" "));
    }

    public static Stream<String> escapeStream(List<String> args, String... additional) {
        return Stream.concat(args.stream(), Stream.of(additional)).map(RunsUtil::escape);
    }

    public static Stream<String> deduplicateElementsFollowingEachOther(Stream<String> stream) {
        return stream.reduce(
                new ArrayList<>(),
                (BiFunction<List<String>, String, List<String>>) (strings, s) -> {
                    if (s.isEmpty()) {
                        return strings;
                    }

                    if (strings.isEmpty()) {
                        strings.add(s);
                        return strings;
                    }

                    if (strings.get(strings.size() - 1).equals(s)) {
                        return strings;
                    }

                    strings.add(s);
                    return strings;
                }, (strings, strings2) -> {
                    strings.addAll(strings2);
                    return strings;
                }).stream();
    }

    /**
     * This expects users to escape quotes in their system arguments on their own, which matches
     * Gradles own behavior when used in JavaExec.
     */
    private static String escape(String arg) {
        return escapeJvmArg(arg);
    }

    public record PreparedUnitTestEnvironment(File programArgumentsFile, File jvmArgumentsFile) {
    }

    public static PreparedUnitTestEnvironment prepareUnitTestEnvironment(Run run) {

        return new PreparedUnitTestEnvironment(
                createArgsFile(run.getWorkingDirectory().file("%s_test_args.txt".formatted(run.getName())), run.getArguments()),
                createArgsFile(run.getWorkingDirectory().file("%s_jvm_args.txt".formatted(run.getName())), run.getJvmArguments())
        );
    }

    private static File createArgsFile(Provider<RegularFile> outputFile, ListProperty<String> inputs) {
        final File output = outputFile.get().getAsFile();

        if (!output.getParentFile().exists()) {
            if (!output.getParentFile().mkdirs()) {
                throw new RuntimeException("Failed to create directory: " + output.getParentFile());
            }
        }
        try {
            final List<String> value = deduplicateElementsFollowingEachOther(inputs.get().stream()).toList();
            if (output.exists()) {
                if (Files.readAllLines(output.toPath()).equals(value)) {
                    return output;
                }

                if (!output.delete()) {
                    throw new RuntimeException("Failed to delete file: " + output);
                }
            }

            Files.write(output.toPath(), value, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return output;
    }

    private static void configureTestTask(Project project, TaskProvider<Test> testTaskProvider, Run run) {
        testTaskProvider.configure(testTask -> {
            PreparedUnitTestEnvironment preparedEnvironment = prepareUnitTestEnvironment(run);

            addRunSourcesDependenciesToTask(testTask, run, true);
            testTask.getDependsOn().add(run.getDependsOn());
            testTask.getDependsOn().add(run.getPostSyncTasks()); //We need this additionally here in case the user runs this through gradle without an IDE

            testTask.setWorkingDir(run.getWorkingDirectory().get());
            testTask.getSystemProperties().putAll(run.getSystemProperties().get());
            testTask.getSystemProperties().put("fml.junit.argsfile", preparedEnvironment.programArgumentsFile().getAbsolutePath());

            testTask.useJUnitPlatform();
            testTask.setGroup("verification");
            testTask.systemProperties(run.getSystemProperties().get());
            testTask.getEnvironment().putAll(run.getEnvironmentVariables().get());
            testTask.setJvmArgs(run.getJvmArguments().get());
            testTask.jvmArgs("@%s".formatted(preparedEnvironment.jvmArgumentsFile().getAbsolutePath()));

            final ConfigurableFileCollection testCP = project.files();
            testCP.from(run.getDependencies().getRuntimeConfiguration());
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

    public static void addRunSourcesDependenciesToTask(Task task, Run run, final boolean requireCompile) {
        for (SourceSet sourceSet : run.getModSources().all().get().values()) {
            final Project sourceSetProject = SourceSetUtils.getProject(sourceSet);

            //The following tasks are not guaranteed to be in the source sets build dependencies
            //We however need at least the classes as well as the resources of the source set to be run
            task.dependsOn(sourceSetProject.getTasks().named(sourceSet.getProcessResourcesTaskName()));
            if (requireCompile) {
                //When running through the IDE we do not need to compile the IDE already will take care of this if need be.
                task.dependsOn(sourceSetProject.getTasks().named(sourceSet.getCompileJavaTaskName()));
            }

            //There might be additional tasks that are needed to configure and run a source set.
            //Also run those
            //Exclude the compileJava and classes tasks, as they are already added above, if need be.
            sourceSet.getOutput().getBuildDependencies().getDependencies(null).stream()
                    .filter(depTask -> !depTask.getName().equals(sourceSet.getCompileJavaTaskName()))
                    .filter(depTask -> !depTask.getName().equals(sourceSet.getClassesTaskName()))
                    .forEach(task::dependsOn);
        }
    }

    public static Provider<String> buildGradleModClasses(final Provider<Multimap<String, SourceSet>> sourceSetsProperty) {
        return buildModClasses(sourceSetsProperty, sourceSet -> Stream.concat(Stream.of(sourceSet.getOutput().getResourcesDir()), sourceSet.getOutput().getClassesDirs().getFiles().stream()));
    }

    public static boolean isRunWithIdea(final SourceSet sourceSet) {
        final Project project = SourceSetUtils.getProject(sourceSet);
        return isRunWithIdea(project);
    }

    public static boolean isRunWithIdea(final Project project) {
        final IdeaModel rootIdeaModel = project.getRootProject().getExtensions().getByType(IdeaModel.class);
        final IdeaRunsExtension ideaRunsExtension = ((ExtensionAware) rootIdeaModel.getProject()).getExtensions().getByType(IdeaRunsExtension.class);

        return ideaRunsExtension.getRunWithIdea().get();
    }

    /**
     * Convert a project and source set to an IntelliJ module name.
     * Same logic as in MDG, except we extract the sourcesets project from the sourceset directly, preventing issues with cross project sourcesets.
     */
    public static String getIntellijModuleName(SourceSet sourceSet) {
        final Project project = SourceSetUtils.getProject(sourceSet);

        var moduleName = new StringBuilder();
        // The `replace` call here is our bug fix compared to ModuleRef!
        // The actual IDEA logic is more complicated, but this should cover the majority of use cases.
        // See https://github.com/JetBrains/intellij-community/blob/a32fd0c588a6da11fd6d5d2fb0362308da3206f3/plugins/gradle/src/org/jetbrains/plugins/gradle/service/project/GradleProjectResolverUtil.java#L205
        // which calls https://github.com/JetBrains/intellij-community/blob/a32fd0c588a6da11fd6d5d2fb0362308da3206f3/platform/util-rt/src/com/intellij/util/PathUtilRt.java#L120
        moduleName.append(project.getRootProject().getName().replace(" ", "_"));
        if (project != project.getRootProject()) {
            moduleName.append(project.getPath().replaceAll(":", "."));
        }
        moduleName.append(".");
        moduleName.append(sourceSet.getName());
        return moduleName.toString();
    }

    @Language("xpath")
    public static final String IDEA_OUTPUT_XPATH = "/project/component[@name='ProjectRootManager']/output/@url";

    public static Provider<Directory> getDefaultIdeaProjectOutDirectory(final Project project) {
        File ideaDir = getIntellijProjectDir(project);
        if (ideaDir == null) {
            throw new IllegalStateException("Could not find IntelliJ project directory for project " + project);
        }

        // Find configured output path
        File miscXml = new File(ideaDir, "misc.xml");
        String outputDirUrl = evaluateXPath(miscXml, IDEA_OUTPUT_XPATH);
        if (outputDirUrl == null) {
            // Apparently IntelliJ defaults to out/ now?
            outputDirUrl = "file://$PROJECT_DIR$/out";
        }

        // The output dir can start with something like "//C:\"; File can handle it.
        final String outputDirTemplate = outputDirUrl.replaceAll("^file:", "");
        return project.getLayout().dir(project.provider(() -> new File(outputDirTemplate.replace("$PROJECT_DIR$", project.getProjectDir().getAbsolutePath()))));
    }

    /**
     * Try to find the IntelliJ project directory that belongs to this Gradle project.
     * There are scenarios where this is impossible, since IntelliJ allows adding
     * Gradle builds to IntelliJ projects in a completely different directory.
     */
    @Nullable
    public static File getIntellijProjectDir(Project project) {
        // Always try the root of a composite build first, since it has the highest chance
        var root = project.getGradle().getParent();
        if (root != null) {
            while (root.getParent() != null) {
                root = root.getParent();
            }

            return getIntellijProjectDir(root.getRootProject().getProjectDir());
        }

        // As a fallback or in case of not using composite builds, try the root project folder
        return getIntellijProjectDir(project.getRootDir());
    }

    @Nullable
    private static File getIntellijProjectDir(File gradleProjectDir) {
        var ideaDir = new File(gradleProjectDir, ".idea");
        return ideaDir.exists() ? ideaDir : null;
    }

    @SuppressWarnings("SameParameterValue")
    @Nullable
    private static String evaluateXPath(File file, @Language("xpath") String expression) {
        try (var fis = new FileInputStream(file)) {
            String result = XPathFactory.newInstance().newXPath().evaluate(expression, new InputSource(fis));
            return result.isBlank() ? null : result;
        } catch (FileNotFoundException | XPathExpressionException ignored) {
            return null;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to evaluate xpath " + expression + " on file " + file, e);
        }
    }

    public static Provider<Directory> getIdeaModuleOutDirectory(final SourceSet sourceSet, final IdeaCompileType ideaCompileType) {
        final Project project = SourceSetUtils.getProject(sourceSet);
        final IdeaModel ideaModel = project.getExtensions().getByType(IdeaModel.class);
        final IdeaModule ideaModule = ideaModel.getModule();

        return ideaCompileType.getSourceSetOutputDirectory(sourceSet, ideaModule);
    }

    public static Provider<? extends FileSystemLocation> getRunWithIdeaDirectory(final SourceSet sourceSet, final IdeaCompileType compileType, final String name) {
        return getIdeaModuleOutDirectory(sourceSet, compileType).map(dir -> dir.dir(name));
    }

    public static Provider<? extends FileSystemLocation> getRunWithIdeaResourcesDirectory(final SourceSet sourceSet) {
        //When running with idea we forcefully redirect all sourcesets to a directory in build, to prevent issues
        //with unit tests started from the gutter -> We can only have a single task, that should run always, regardless of run or sourceset:
        final Project project = SourceSetUtils.getProject(sourceSet);
        final ProjectLayout buildLayout = project.getLayout();
        return buildLayout.getBuildDirectory().map(dir -> dir.dir("idea").dir("resources").dir(sourceSet.getName()));
    }

    public static Provider<? extends FileSystemLocation> getRunWithIdeaClassesDirectory(final SourceSet sourceSet, final IdeaCompileType compileType) {
        return getRunWithIdeaDirectory(sourceSet, compileType, "classes");
    }

    public static Provider<String> buildRunWithIdeaModClasses(
            final Provider<Multimap<String, SourceSet>> compileSourceSets,
            final IdeaCompileType compileType) {
        return buildModClasses(compileSourceSets, sourceSet -> {

            if (isRunWithIdea(sourceSet)) {
                final File resourcesDir = getRunWithIdeaResourcesDirectory(sourceSet).get().getAsFile();
                final File classesDir = getRunWithIdeaClassesDirectory(sourceSet, compileType).get().getAsFile();
                return Stream.of(resourcesDir, classesDir);
            }

            return Stream.concat(Stream.of(sourceSet.getOutput().getResourcesDir()), sourceSet.getOutput().getClassesDirs().getFiles().stream());
        });
    }

    @SuppressWarnings("UnstableApiUsage")
    public static Provider<String> buildRunWithEclipseModClasses(final Provider<Multimap<String, SourceSet>> compileSourceSets) {
        return buildModClasses(compileSourceSets, sourceSet -> {
            final Project project = SourceSetUtils.getProject(sourceSet);
            final EclipseModel eclipseModel = project.getExtensions().getByType(EclipseModel.class);

            final File conventionsDir = new File(project.getProjectDir(), "bin");
            eclipseModel.getClasspath().getBaseSourceOutputDir().convention(project.provider(() -> conventionsDir));

            final File parentDir = eclipseModel.getClasspath().getBaseSourceOutputDir().get();
            final File sourceSetDir = new File(parentDir, sourceSet.getName());
            return Stream.of(sourceSetDir);
        });
    }

    public static Provider<String> buildModClasses(final Provider<Multimap<String, SourceSet>> compileSourceSets,
                                                   final Function<SourceSet, Stream<File>> directoryBuilder) {
        return compileSourceSets.map(sourceSetsByRunId -> sourceSetsByRunId.entries().stream().flatMap(entry ->
                        directoryBuilder.apply(entry.getValue())
                                .peek(File::mkdirs)
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

    public static String escapeJvmArg(String arg) {
        var escaped = arg.replace("\\", "\\\\").replace("\"", "\\\"");
        if (escaped.contains(" ")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    public enum IdeaCompileType {
        Production,
        Test;

        public boolean isTest() {
            return this == Test;
        }

        public File getOutputDir(IdeaModule module) {
            return isTest() ? module.getTestOutputDir() : module.getOutputDir();
        }

        public Provider<Directory> getSourceSetOutputDirectory(SourceSet sourceSet, IdeaModule ideaModule) {
            if (getOutputDir(ideaModule) != null) {
                return ideaModule.getProject().getLayout().dir(
                        ideaModule.getProject().provider(() -> getOutputDir(ideaModule))
                );
            }

            final Provider<Directory> projectOut = getDefaultIdeaProjectOutDirectory(ideaModule.getProject());
            if (ideaModule.getInheritOutputDirs() == null || !ideaModule.getInheritOutputDirs()) {
                final String sourceSetName = SourceSet.MAIN_SOURCE_SET_NAME.equals(sourceSet.getName()) ?
                        "production" : sourceSet.getName();

                return projectOut.map(dir -> dir.dir(sourceSetName));
            }

            final String compileTypeDirectory = this.name().toLowerCase(Locale.ROOT);
            final String moduleName = getIntellijModuleName(sourceSet);

            return projectOut.map(dir -> dir.dir(compileTypeDirectory).dir(moduleName));
        }
    }
}
