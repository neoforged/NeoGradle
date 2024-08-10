package net.neoforged.gradle.common.runs.ide;

import com.google.common.collect.Multimap;
import net.neoforged.elc.configs.GradleLaunchConfig;
import net.neoforged.elc.configs.JavaApplicationLaunchConfig;
import net.neoforged.elc.configs.LaunchConfig;
import net.neoforged.elc.configs.LaunchGroup;
import net.neoforged.gradle.common.extensions.IdeManagementExtension;
import net.neoforged.gradle.common.runs.ide.extensions.IdeaRunExtensionImpl;
import net.neoforged.gradle.common.runs.ide.idea.JUnitWithBeforeRun;
import net.neoforged.gradle.common.runs.run.RunImpl;
import net.neoforged.gradle.common.util.ProjectUtils;
import net.neoforged.gradle.common.util.SourceSetUtils;
import net.neoforged.gradle.common.util.run.RunsUtil;
import net.neoforged.gradle.dsl.common.extensions.subsystems.Subsystems;
import net.neoforged.gradle.dsl.common.extensions.subsystems.conventions.ide.IDEA;
import net.neoforged.gradle.dsl.common.runs.ide.extensions.IdeaRunExtension;
import net.neoforged.gradle.dsl.common.runs.idea.extensions.IdeaRunsExtension;
import net.neoforged.gradle.dsl.common.runs.run.Run;
import net.neoforged.gradle.dsl.common.runs.run.RunManager;
import net.neoforged.gradle.dsl.common.util.CommonRuntimeUtils;
import net.neoforged.gradle.util.FileUtils;
import net.neoforged.vsclc.BatchedLaunchWriter;
import net.neoforged.vsclc.LaunchConfiguration;
import net.neoforged.vsclc.attribute.PathLike;
import net.neoforged.vsclc.attribute.ShortCmdBehaviour;
import net.neoforged.vsclc.writer.WritingMode;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.language.jvm.tasks.ProcessResources;
import org.gradle.plugins.ide.eclipse.model.EclipseModel;
import org.gradle.plugins.ide.idea.model.IdeaModel;
import org.gradle.plugins.ide.idea.model.IdeaProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.gradle.ext.*;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * A simple manager which configures runs based on the IDE it is attached to.
 */
public class IdeRunIntegrationManager {
    
    private static final IdeRunIntegrationManager INSTANCE = new IdeRunIntegrationManager();
    
    public static IdeRunIntegrationManager getInstance() {
        return INSTANCE;
    }
    
    private IdeRunIntegrationManager() {
    }

    /**
     * Configures the IDE integration DSLs.
     *
     * @param project The project to configure.
     */
    public void setup(final Project project) {
        project.getExtensions().configure(RunManager.class, runs -> runs.configureAll(run -> {
            setupRun(project, run);
        }));
        
        final Project rootProject = project.getRootProject();
        final IdeaModel ideaModel = rootProject.getExtensions().getByType(IdeaModel.class);
        final IdeaProject ideaProject = ideaModel.getProject();
        final ExtensionAware extensionAware = (ExtensionAware) ideaProject;
        if (extensionAware.getExtensions().findByType(IdeaRunsExtension.class) == null && extensionAware.getExtensions().findByName("runs") == null) {
            extensionAware.getExtensions().create("runs", IdeaRunsExtension.class, project);
        }
    }

    public void setupRun(Project project, Run run) {
        run.getExtensions().create(IdeaRunExtension.class, "idea", IdeaRunExtensionImpl.class, project, run);
    }

    /**
     * Configures the IDE integration to run runs as tasks from the IDE.
     *
     * @param project The project to configure.
     */
    public void apply(final Project project) {
        final IdeManagementExtension ideManager = project.getExtensions().getByType(IdeManagementExtension.class);
        project.afterEvaluate(evaluatedProject -> {
            ideManager.apply(new RunsImportAction());
        });
    }

    public void configureIdeaConventions(Project project, IDEA ideaConventions) {
        final Project rootProject = project.getRootProject();
        final IdeaModel ideaModel = rootProject.getExtensions().getByType(IdeaModel.class);
        final IdeaProject ideaProject = ideaModel.getProject();
        final ExtensionAware extensionAware = (ExtensionAware) ideaProject;
        final IdeaRunsExtension runsExtension = extensionAware.getExtensions().getByType(IdeaRunsExtension.class);

        runsExtension.getRunWithIdea().convention(
                ideaConventions.getShouldUseCompilerDetection()
                        .map(useCompilerDetection -> {
                            if (!useCompilerDetection) {
                                return false;
                            }

                            final File DotIdeaDirectory = new File(project.getProjectDir(), ".idea");
                            final File GradleXml = new File(DotIdeaDirectory, "gradle.xml");
                            return FileUtils.contains(GradleXml, "<option name=\"delegatedBuild\" value=\"false\" />");
                        })
        );
    }

    private static final class RunsImportAction implements IdeManagementExtension.IdeImportAction {

        private void common(Project project) {
            final SourceSetContainer container = project.getExtensions().getByType(SourceSetContainer.class);
            container.forEach(sourceSet -> {
                final Configuration compileClasspath = project.getConfigurations().getByName(sourceSet.getCompileClasspathConfigurationName());
                final Configuration runtimeClasspath = project.getConfigurations().getByName(sourceSet.getRuntimeClasspathConfigurationName());

                compileClasspath.resolve();
                runtimeClasspath.resolve();
            });
        }

        @Override
        public void idea(Project project, Project rootProject, IdeaModel idea, ProjectSettings ideaExtension) {
            project.afterEvaluate(evaluatedProject -> {
                common(project);

                final RunConfigurationContainer ideaRuns = ((ExtensionAware) ideaExtension).getExtensions().getByType(RunConfigurationContainer.class);

                attemptJUnitWithBeforeRunRegistration(project, ideaRuns);

                project.getExtensions().configure(RunManager.class, runs -> runs.all(run -> createIdeaRun(project, run, ideaRuns, false)));

                if (project.getExtensions().getByType(Subsystems.class).getConventions().getIde().getIdea().getShouldReconfigureTemplatesForTests().get()) {
                    final ExtensionAware ideaModelExtensions = (ExtensionAware) idea;
                    final Run ideaDefaultUnitTestRun = ideaModelExtensions.getExtensions().getByType(Run.class);

                    //We finally know that the user wants this to be registered, additionally the IDE integration resolved the lazy dependencies
                    //here, so we can now with a gentle heart register the run type, and the run configuration conversion.
                    RunManager runManager = project.getExtensions().getByType(RunManager.class);
                    runManager.addInternal(ideaDefaultUnitTestRun);

                    createIdeaRun(project, ideaDefaultUnitTestRun, ideaRuns, true);
                }
            });
        }

        private static void attemptJUnitWithBeforeRunRegistration(Project project, RunConfigurationContainer ideaRuns) {
            try {
                ideaRuns.registerFactory(JUnitWithBeforeRun.class, ideaTaskName -> project.getObjects().newInstance(JUnitWithBeforeRun.class, project, ideaTaskName));
            } catch (GradleException ignored) {
                //Gets thrown when the factory is already registered
            }
        }

        private void createIdeaRun(Project project, Run run, RunConfigurationContainer ideaRuns, boolean defaultRun) {
            if (!defaultRun && !run.getShouldExportToIDE().get())
                return;

            final String nameWithoutSpaces = run.getName().replace(" ", "-");
            final String runName = StringUtils.capitalize(project.getName() + ": " + StringUtils.capitalize(nameWithoutSpaces));

            final RunImpl runImpl = (RunImpl) run;

            final TaskProvider<?> ideBeforeRunTask = getOrCreateIdeBeforeRunTask(project, runImpl);

            if (RunsUtil.isRunWithIdea(project)) {
                final List<TaskProvider<?>> copyProcessResourcesTasks = createIntelliJCopyResourcesTasks(run);

                ideBeforeRunTask.configure(task -> copyProcessResourcesTasks.forEach(task::dependsOn));
            }

            //Do not generate a run configuration for unit tests
            if (!runImpl.getIsJUnit().get()) {
                ideaRuns.register(runName, Application.class, ideaRun -> {
                    runImpl.getWorkingDirectory().get().getAsFile().mkdirs();

                    ideaRun.setMainClass(runImpl.getMainClass().get());
                    ideaRun.setWorkingDirectory(runImpl.getWorkingDirectory().get().getAsFile().getAbsolutePath());
                    ideaRun.setJvmArgs(RunsUtil.escapeAndJoin(runImpl.realiseJvmArguments()));
                    ideaRun.setModuleName(RunsUtil.getIntellijModuleName(run.getExtensions().getByType(IdeaRunExtension.class).getPrimarySourceSet().get()));
                    ideaRun.setProgramParameters(RunsUtil.escapeAndJoin(runImpl.getArguments().get()));
                    ideaRun.setEnvs(adaptEnvironment(runImpl, multimapProvider -> RunsUtil.buildRunWithIdeaModClasses(multimapProvider, RunsUtil.IdeaCompileType.Production)));
                    ideaRun.setShortenCommandLine(ShortenCommandLine.ARGS_FILE);

                    ideaRun.beforeRun(beforeRuns -> {
                        beforeRuns.create("Build", Make.class);

                        beforeRuns.create("Prepare Run", GradleTask.class, gradleTask -> {
                            gradleTask.setTask(ideBeforeRunTask.get());
                        });
                    });

                    ideaRun.setDefaults(defaultRun);
                });
            } else {
                ideaRuns.register(runName, JUnitWithBeforeRun.class, ideaRun -> {
                    final RunsUtil.PreparedUnitTestEnvironment preparedUnitTestEnvironment = RunsUtil.prepareUnitTestEnvironment(run);

                    ideaRun.setWorkingDirectory(runImpl.getWorkingDirectory().get().getAsFile().getAbsolutePath());
                    ideaRun.setModuleName(RunsUtil.getIntellijModuleName(run.getExtensions().getByType(IdeaRunExtension.class).getPrimarySourceSet().get()));

                    ideaRun.setPackageName(runImpl.getTestScope().getPackageName().getOrNull());
                    ideaRun.setDirectory(runImpl.getTestScope().getDirectory().map(dir -> dir.getAsFile().getAbsolutePath()).getOrNull());
                    ideaRun.setPattern(runImpl.getTestScope().getPattern().getOrNull());
                    ideaRun.setClassName(runImpl.getTestScope().getClassName().getOrNull());
                    ideaRun.setMethod(runImpl.getTestScope().getMethod().getOrNull());
                    ideaRun.setCategory(runImpl.getTestScope().getCategory().getOrNull());

                    ideaRun.setWorkingDirectory(runImpl.getWorkingDirectory().get().getAsFile().getAbsolutePath());
                    ideaRun.setVmParameters(RunsUtil.escapeAndJoin(runImpl.realiseJvmArguments(),
                                    "-Dfml.junit.argsfile=%s".formatted(preparedUnitTestEnvironment.programArgumentsFile().getAbsolutePath()),
                                    "@%s".formatted(preparedUnitTestEnvironment.jvmArgumentsFile().getAbsolutePath())
                    ));
                    ideaRun.setEnvs(adaptEnvironment(runImpl,
                            multimapProvider -> RunsUtil.buildRunWithIdeaModClasses(multimapProvider, RunsUtil.IdeaCompileType.Production),
                            multimapProvider -> RunsUtil.buildRunWithIdeaModClasses(multimapProvider, RunsUtil.IdeaCompileType.Test))
                    );
                    ideaRun.setShortenCommandLine(ShortenCommandLine.ARGS_FILE);

                    ideaRun.beforeRun(beforeRuns -> {
                        beforeRuns.create("Build", Make.class);

                        beforeRuns.create("Prepare JUnit Test", GradleTask.class, gradleTask -> {
                            gradleTask.setTask(ideBeforeRunTask.get());
                        });
                    });

                    ideaRun.setDefaults(defaultRun);
                });
            }
        }

        @Override
        public void eclipse(Project project, EclipseModel eclipse) {
            ProjectUtils.afterEvaluate(project, () -> {
                common(project);

                project.getExtensions().configure(RunManager.class, runs -> runs.all(run -> {
                    if (!run.getShouldExportToIDE().get())
                        return;

                    final String name = run.getName();
                    final String runName = StringUtils.capitalize(project.getName() + " - " + StringUtils.capitalize(name.replace(" ", "-")));
                    
                    final RunImpl runImpl = (RunImpl) run;

                    //Do not generate a run configuration for unit tests
                    if (runImpl.getIsJUnit().get())
                        return;

                    final TaskProvider<?> ideBeforeRunTask = getOrCreateIdeBeforeRunTask(project, runImpl);
                    final List<TaskProvider<?>> copyProcessResourcesTasks = createEclipseCopyResourcesTasks(eclipse, run);
                    ideBeforeRunTask.configure(task -> copyProcessResourcesTasks.forEach(task::dependsOn));
                    
                    try {
                        final GradleLaunchConfig idePreRunTask = GradleLaunchConfig.builder(eclipse.getProject().getName())
                                                                         .tasks(ideBeforeRunTask.get().getName())
                                                                         .build();
                        
                        final String gradleName = "Prepare " + runName;
                        writeLaunchToFile(project, gradleName, idePreRunTask);
                        
                        final JavaApplicationLaunchConfig debugRun =
                                JavaApplicationLaunchConfig.builder(eclipse.getProject().getName())
                                        .workingDirectory(runImpl.getWorkingDirectory().get().getAsFile().getAbsolutePath())
                                        .vmArgs(RunsUtil.escapeStream(runImpl.realiseJvmArguments()).toArray(String[]::new))
                                        .args(RunsUtil.escapeStream(runImpl.getArguments().get()).toArray(String[]::new))
                                        .envVar(adaptEnvironment(runImpl, RunsUtil::buildRunWithEclipseModClasses))
                                        .useArgumentsFile()
                                        .build(runImpl.getMainClass().get());
                        
                        final String debugName = "Run " + runName;
                        writeLaunchToFile(project, debugName, debugRun);
                        
                        writeLaunchToFile(project, runName,
                                LaunchGroup.builder()
                                        .entry(LaunchGroup.entry(gradleName)
                                                       .enabled(true)
                                                       .adoptIfRunning(false)
                                                       .mode(LaunchGroup.Mode.RUN)
                                                       .action(LaunchGroup.Action.delay(2)))
                                        .entry(LaunchGroup.entry(debugName)
                                                       .enabled(true)
                                                       .adoptIfRunning(false)
                                                       .mode(LaunchGroup.Mode.DEBUG)
                                                       .action(LaunchGroup.Action.none()))
                                        .build());
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to write launch files: " + runName, e);
                    }
                }));
            });
        }

        @Override
        public void vscode(Project project, EclipseModel eclipse)
        {
            ProjectUtils.afterEvaluate(project, () -> {
                common(project);

                final BatchedLaunchWriter launchWriter = new BatchedLaunchWriter(WritingMode.MODIFY_CURRENT);
                project.getExtensions().configure(RunManager.class, runs -> runs.all(run -> {
                    if (!run.getShouldExportToIDE().get())
                        return;

                    final String name = run.getName();
                    final String runName = StringUtils.capitalize(project.getName() + " - " + StringUtils.capitalize(name.replace(" ", "-")));

                    final RunImpl runImpl = (RunImpl) run;
                    final TaskProvider<?> ideBeforeRunTask = getOrCreateIdeBeforeRunTask(project, runImpl);

                    final List<TaskProvider<?>> copyProcessResourcesTasks = createEclipseCopyResourcesTasks(eclipse, run);
                    ideBeforeRunTask.configure(task -> copyProcessResourcesTasks.forEach(task::dependsOn));

                    final LaunchConfiguration cfg = launchWriter.createGroup("NG - " + project.getName(), WritingMode.REMOVE_EXISTING)
                        .createLaunchConfiguration()
                        .withAdditionalJvmArgs(runImpl.realiseJvmArguments())
                        .withArguments(runImpl.getArguments().get())
                        .withCurrentWorkingDirectory(PathLike.ofNio(runImpl.getWorkingDirectory().get().getAsFile().toPath()))
                        .withEnvironmentVariables(adaptEnvironment(runImpl, RunsUtil::buildRunWithEclipseModClasses))
                        .withShortenCommandLine(ShortCmdBehaviour.ARGUMENT_FILE)
                        .withMainClass(runImpl.getMainClass().get())
                        .withProjectName(eclipse.getProject().getName())
                        .withName(runName);

                    if (IdeManagementExtension.isVscodePluginImport(project))
                    {
                        cfg.withPreTaskName("gradle: " + ideBeforeRunTask.getName());
                    }
                    else
                    {
                        eclipse.autoBuildTasks(ideBeforeRunTask);
                    }
                }));
                try {
                    launchWriter.writeToLatestJson(project.getRootDir().toPath());
                } catch (final IOException e) {
                    throw new RuntimeException("Failed to write launch files", e);
                }
            });
        }

        private TaskProvider<?> getOrCreateIdeBeforeRunTask(Project project, RunImpl run) {
            if (project.getTasks().getNames().contains("ideBeforeRun")) {
                 final TaskProvider<?> provider = project.getTasks().named("ideBeforeRun");
                 provider.configure(task -> {
                     RunsUtil.addRunSourcesDependenciesToTask(task, run, false);
                     task.getDependsOn().add(run.getDependsOn());
                 });
                 return provider;
            }

            return project.getTasks().register("ideBeforeRun", task -> {
                RunsUtil.addRunSourcesDependenciesToTask(task, run, false);
                task.getDependsOn().add(run.getDependsOn());
            });
        }

        private List<TaskProvider<?>> createIntelliJCopyResourcesTasks(Run run) {
            final List<TaskProvider<?>> copyProcessResources = new ArrayList<>();
            for (SourceSet sourceSet : run.getModSources().all().get().values()) {
                copyProcessResources.add(setupCopyResourcesForIdea(sourceSet));
            }

            if (run.getIsJUnit().get()) {
                for (SourceSet sourceSet : run.getUnitTestSources().all().get().values()) {
                    copyProcessResources.add(setupCopyResourcesForIdea(sourceSet));
                }
            }

            return copyProcessResources;
        }

        private static @NotNull TaskProvider<?> setupCopyResourcesForIdea(SourceSet sourceSet) {
            final Project sourceSetProject = SourceSetUtils.getProject(sourceSet);

            final String taskName = CommonRuntimeUtils.buildTaskName("intelliJCopy", sourceSet.getProcessResourcesTaskName());
            final TaskProvider<?> intelliJResourcesTask;

            if (sourceSetProject.getTasks().findByName(taskName) != null) {
                intelliJResourcesTask = sourceSetProject.getTasks().named(taskName);
            }
            else {
                intelliJResourcesTask = sourceSetProject.getTasks().register(taskName, Copy.class, task -> {
                    final TaskProvider<ProcessResources> defaultProcessResources = sourceSetProject.getTasks().named(sourceSet.getProcessResourcesTaskName(), ProcessResources.class);
                    task.from(defaultProcessResources.map(ProcessResources::getDestinationDir));
                    task.into(RunsUtil.getRunWithIdeaResourcesDirectory(sourceSet));

                    task.dependsOn(defaultProcessResources);
                });
            }
            return intelliJResourcesTask;
        }

        private List<TaskProvider<?>> createEclipseCopyResourcesTasks(EclipseModel eclipse, Run run) {
            final List<TaskProvider<?>> copyProcessResources = new ArrayList<>();
            for (SourceSet sourceSet : run.getModSources().all().get().values()) {
                final Project sourceSetProject = SourceSetUtils.getProject(sourceSet);

                final String taskName = CommonRuntimeUtils.buildTaskName("eclipseCopy", sourceSet.getProcessResourcesTaskName());
                final TaskProvider<?> eclipseResourcesTask;

                if (sourceSetProject.getTasks().findByName(taskName) != null) {
                    eclipseResourcesTask = sourceSetProject.getTasks().named(taskName);
                }
                else {
                    eclipseResourcesTask = sourceSetProject.getTasks().register(taskName, Copy.class, task -> {
                        final TaskProvider<ProcessResources> defaultProcessResources = sourceSetProject.getTasks().named(sourceSet.getProcessResourcesTaskName(), ProcessResources.class);
                        task.from(defaultProcessResources.map(ProcessResources::getDestinationDir));
                        Path outputDir = eclipse.getClasspath().getDefaultOutputDir().toPath();
                        if (outputDir.endsWith("default")) {
                            // sometimes it has default value from org.gradle.plugins.ide.eclipse.internal.EclipsePluginConstants#DEFAULT_PROJECT_OUTPUT_PATH
                            // which has /default on end that is not present in the final outputDir in eclipse/buildship
                            // (output of getDefaultOutputDir() should be just project/bin/)
                            outputDir = outputDir.getParent();
                        }
                        task.into(outputDir.resolve(sourceSet.getName()));

                        task.dependsOn(defaultProcessResources);
                    });
                }

                copyProcessResources.add(eclipseResourcesTask);
            }
            return copyProcessResources;
        }

        private static void writeLaunchToFile(Project project, String fileName, LaunchConfig config) {
            final File file = project.file(String.format(".eclipse/configurations/%s.launch", fileName));
            file.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(file, false)) {
                config.write(writer);
            }
            catch (IOException e) {
                throw new UncheckedIOException("Failed to write launch file: " + fileName, e);
            }
            catch (XMLStreamException e) {
                throw new RuntimeException("Failed to write launch file: " + fileName, e);
            }
        }
        
        private static Map<String, String> adaptEnvironment(
                final RunImpl run,
                final Function<Provider<Multimap<String, SourceSet>>, Provider<String>> modClassesProvider
                ) {
            final Map<String, String> environment = new HashMap<>(run.getEnvironmentVariables().get());
            environment.put("MOD_CLASSES", modClassesProvider.apply(run.getModSources().all()).get());
            return environment;
        }

        private static Map<String, String> adaptEnvironment(
                final RunImpl run,
                final Function<Provider<Multimap<String, SourceSet>>, Provider<String>> modClassesProvider,
                final Function<Provider<Multimap<String, SourceSet>>, Provider<String>> modTestClassesProvider
        ) {
            final Map<String, String> environment = new HashMap<>(run.getEnvironmentVariables().get());

            final Provider<String> joinedModClasses = modClassesProvider.apply(run.getModSources().all())
                    .zip(modTestClassesProvider.apply(run.getUnitTestSources().all()), (modClasses, testClasses) -> modClasses + File.pathSeparator + testClasses);

            environment.put("MOD_CLASSES", joinedModClasses.get());
            return environment;
        }
    }
}
