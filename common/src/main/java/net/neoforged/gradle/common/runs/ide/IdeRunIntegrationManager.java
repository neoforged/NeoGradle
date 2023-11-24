package net.neoforged.gradle.common.runs.ide;

import cz.nightenom.vsclaunch.BatchedLaunchWriter;
import cz.nightenom.vsclaunch.LaunchConfiguration;
import cz.nightenom.vsclaunch.attribute.PathLike;
import cz.nightenom.vsclaunch.attribute.ShortCmdBehaviour;
import cz.nightenom.vsclaunch.writer.WritingMode;
import net.neoforged.elc.configs.GradleLaunchConfig;
import net.neoforged.elc.configs.JavaApplicationLaunchConfig;
import net.neoforged.elc.configs.LaunchConfig;
import net.neoforged.elc.configs.LaunchGroup;
import net.neoforged.gradle.common.extensions.IdeManagementExtension;
import net.neoforged.gradle.common.runs.ide.extensions.IdeaRunExtensionImpl;
import net.neoforged.gradle.common.runs.run.RunImpl;
import net.neoforged.gradle.common.util.ProjectUtils;
import net.neoforged.gradle.common.util.SourceSetUtils;
import net.neoforged.gradle.common.util.constants.RunsConstants;
import net.neoforged.gradle.common.util.run.RunsUtil;
import net.neoforged.gradle.dsl.common.runs.ide.extensions.IdeaRunExtension;
import net.neoforged.gradle.dsl.common.runs.idea.extensions.IdeaRunsExtension;
import net.neoforged.gradle.dsl.common.runs.run.Run;
import net.neoforged.gradle.dsl.common.util.CommonRuntimeUtils;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.language.jvm.tasks.ProcessResources;
import org.gradle.plugins.ide.eclipse.model.EclipseModel;
import org.gradle.plugins.ide.idea.model.IdeaModel;
import org.gradle.plugins.ide.idea.model.IdeaProject;
import org.jetbrains.gradle.ext.*;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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
        project.getExtensions().configure(RunsConstants.Extensions.RUNS, (Action<NamedDomainObjectContainer<Run>>) runs -> runs.configureEach(run -> {
            run.getExtensions().create(IdeaRunExtension.class, "idea", IdeaRunExtensionImpl.class, project, run);
        }));
        
        final Project rootProject = project.getRootProject();
        final IdeaModel ideaModel = rootProject.getExtensions().getByType(IdeaModel.class);
        final IdeaProject ideaProject = ideaModel.getProject();
        final ExtensionAware extensionAware = (ExtensionAware) ideaProject;
        if (extensionAware.getExtensions().findByType(IdeaRunsExtension.class) == null) {
            extensionAware.getExtensions().create("runs", IdeaRunsExtension.class, project);
        }
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
    
    private static final class RunsImportAction implements IdeManagementExtension.IdeImportAction {
        
        @Override
        public void idea(Project project, IdeaModel idea, ProjectSettings ideaExtension) {
            final RunConfigurationContainer ideaRuns = ((ExtensionAware) ideaExtension).getExtensions().getByType(RunConfigurationContainer.class);
            
            project.getExtensions().configure(RunsConstants.Extensions.RUNS, (Action<NamedDomainObjectContainer<Run>>) runs -> runs.getAsMap().forEach((name, run) -> {
                final String runName = StringUtils.capitalize(project.getName() + ": " + StringUtils.capitalize(name.replace(" ", "-")));
                
                final RunImpl runImpl = (RunImpl) run;
                final IdeaRunExtension runIdeaConfig = run.getExtensions().getByType(IdeaRunExtension.class);
                final TaskProvider<?> ideBeforeRunTask = createIdeBeforeRunTask(project, name, run, runImpl, true);
                
                ideaRuns.register(runName, Application.class, ideaRun -> {
                    runImpl.getWorkingDirectory().get().getAsFile().mkdirs();
                    
                    ideaRun.setMainClass(runImpl.getMainClass().get());
                    ideaRun.setWorkingDirectory(runImpl.getWorkingDirectory().get().getAsFile().getAbsolutePath());
                    ideaRun.setJvmArgs(String.join(" ", runImpl.realiseJvmArguments(true)));
                    ideaRun.moduleRef(project, runIdeaConfig.getPrimarySourceSet().get());
                    ideaRun.setProgramParameters(runImpl.getProgramArguments().get()
                                                                          .stream()
                                                                          .map(arg -> "\"" + arg + "\"")
                                                                          .collect(Collectors.joining(" ")));
                    ideaRun.setEnvs(adaptEnvironment(runImpl, RunsUtil::buildRunWithIdeaModClasses));
                    ideaRun.setShortenCommandLine(ShortenCommandLine.ARGS_FILE);
                    
                    ideaRun.beforeRun(beforeRuns -> {
                        beforeRuns.create("Build", Make.class);
                        
                        beforeRuns.create("Prepare Run", GradleTask.class, gradleTask -> {
                            gradleTask.setTask(ideBeforeRunTask.get());
                        });
                    });
                });
            }));
            
            
        }
        
        @Override
        public void eclipse(Project project, EclipseModel eclipse) {
            ProjectUtils.afterEvaluate(project, () -> {
                project.getExtensions().configure(RunsConstants.Extensions.RUNS, (Action<NamedDomainObjectContainer<Run>>) runs -> runs.getAsMap().forEach((name, run) -> {
                    final String runName = StringUtils.capitalize(project.getName() + " - " + StringUtils.capitalize(name.replace(" ", "-")));
                    
                    final RunImpl runImpl = (RunImpl) run;
                    final TaskProvider<?> ideBeforeRunTask = createEclipseBeforeRunTask(eclipse, project, name, run, runImpl);
                    
                    try {
                        final GradleLaunchConfig idePreRunTask = GradleLaunchConfig.builder(eclipse.getProject().getName())
                                                                         .tasks(ideBeforeRunTask.get().getName())
                                                                         .build();
                        
                        final String gradleName = "Prepare " + runName;
                        writeLaunchToFile(project, gradleName, idePreRunTask);
                        
                        final JavaApplicationLaunchConfig debugRun =
                                JavaApplicationLaunchConfig.builder(eclipse.getProject().getName())
                                        .workingDirectory(runImpl.getWorkingDirectory().get().getAsFile().getAbsolutePath())
                                        .vmArgs(runImpl.realiseJvmArguments(true).toArray(new String[0]))
                                        .args(runImpl.getProgramArguments().get()
                                                      .stream()
                                                      .map(arg -> "\"" + arg + "\"")
                                                      .toArray(String[]::new))
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
                final BatchedLaunchWriter launchWriter = new BatchedLaunchWriter(WritingMode.MODIFY_CURRENT);
                project.getExtensions().configure(RunsConstants.Extensions.RUNS, (Action<NamedDomainObjectContainer<Run>>) runs -> runs.getAsMap().forEach((name, run) -> {
                    final String runName = StringUtils.capitalize(project.getName() + " - " + StringUtils.capitalize(name.replace(" ", "-")));
                    final RunImpl runImpl = (RunImpl) run;

                    final TaskProvider<?> ideBeforeRunTask = createEclipseBeforeRunTask(eclipse, project, name, run, runImpl);

                    final LaunchConfiguration cfg = launchWriter.createGroup("NG - " + project.getName(), WritingMode.REMOVE_EXISTING)
                        .createLaunchConfiguration()
                        .withAdditionalJvmArgs(runImpl.realiseJvmArguments(false))
                        .withArguments(runImpl.getProgramArguments().get())
                        .withCurrentWorkingDirectory(PathLike.ofNio(runImpl.getWorkingDirectory().get().getAsFile().toPath()))
                        .withEnvironmentVariables(adaptEnvironment(runImpl, RunsUtil::buildRunWithEclipseModClasses))
                        .withShortenCommandLine(ShortCmdBehaviour.ARGUMENT_FILE)
                        .withMainClass(runImpl.getMainClass().get())
                        .withProjectName(project.getName())
                        .withName(runName);

                    if (IdeManagementExtension.isDefinitelyVscodeImport(project))
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

        private TaskProvider<?> createIdeBeforeRunTask(Project project, String name, Run run, RunImpl runImpl, boolean addDefaultProcessResources) {
            final TaskProvider<?> ideBeforeRunTask = project.getTasks().register(CommonRuntimeUtils.buildTaskName("ideBeforeRun", name), task -> {
                for (SourceSet sourceSet : run.getModSources().get()) {
                    final Project sourceSetProject = SourceSetUtils.getProject(sourceSet);
                    
                    //The following tasks are not guaranteed to be in the source sets build dependencies
                    //We however need at least the classes as well as the resources of the source set to be run
                    if (addDefaultProcessResources) {
                        task.dependsOn(sourceSetProject.getTasks().named(sourceSet.getProcessResourcesTaskName()));
                    }
                    task.dependsOn(sourceSetProject.getTasks().named(sourceSet.getCompileJavaTaskName()));
                    
                    //There might be additional tasks that are needed to configure and run a source set.
                    //Also run those
                    sourceSet.getOutput().getBuildDependencies().getDependencies(null)
                            .forEach(task::dependsOn);
                }
            });
            
            if (!runImpl.getTaskDependencies().isEmpty()) {
                ideBeforeRunTask.configure(task -> {
                    runImpl.getTaskDependencies().forEach(dep -> {
                        //noinspection Convert2MethodRef Creates a compiler error regarding incompatible types.
                        task.dependsOn(dep);
                    });
                });
            }
            
            return ideBeforeRunTask;
        }

        private TaskProvider<?> createEclipseBeforeRunTask(EclipseModel eclipse, Project project, String name, Run run, RunImpl runImpl) {
            final TaskProvider<?> ideBeforeRunTask = createIdeBeforeRunTask(project, name, run, runImpl, false);
            
            for (SourceSet sourceSet : run.getModSources().get()) {
                final Project sourceSetProject = SourceSetUtils.getProject(sourceSet);
                
                //The following tasks are not guaranteed to be in the source sets build dependencies
                //We however need at least the classes as well as the resources of the source set to be run
                final String taskName = CommonRuntimeUtils.buildTaskName("eclipse", sourceSet.getProcessResourcesTaskName());
                final TaskProvider<?> eclipseResourcesTask;
                
                if (sourceSetProject.getTasks().findByName(taskName) != null)
                {
                    eclipseResourcesTask = sourceSetProject.getTasks().named(taskName);
                }
                else
                {
                    eclipseResourcesTask = sourceSetProject.getTasks().register(taskName, Copy.class, t -> {
                        final TaskProvider<ProcessResources> defaultProcessResources =
                            sourceSetProject.getTasks().named(sourceSet.getProcessResourcesTaskName(), ProcessResources.class);
                        t.from(defaultProcessResources.get().getDestinationDir());
                        t.into(eclipse.getClasspath().getDefaultOutputDir().toPath().resolve(sourceSet.getName()));
                    });
                }

                eclipse.autoBuildTasks(eclipseResourcesTask);
            }
            
            return ideBeforeRunTask;
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
                final Function<ListProperty<SourceSet>, Provider<String>> modClassesProvider
                ) {
            final Map<String, String> environment = new HashMap<>(run.getEnvironmentVariables().get());
            environment.put("MOD_CLASSES", modClassesProvider.apply(run.getModSources()).get());
            return environment;
        }
    }
}
