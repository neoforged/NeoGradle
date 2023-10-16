package net.neoforged.gradle.common.runs.ide;

import net.neoforged.elc.configs.GradleLaunchConfig;
import net.neoforged.elc.configs.JavaApplicationLaunchConfig;
import net.neoforged.elc.configs.LaunchConfig;
import net.neoforged.elc.configs.LaunchGroup;
import net.neoforged.gradle.common.extensions.IdeManagementExtension;
import net.neoforged.gradle.common.runs.ide.extensions.IdeaRunExtensionImpl;
import net.neoforged.gradle.common.runs.run.RunImpl;
import net.neoforged.gradle.common.util.ProjectUtils;
import net.neoforged.gradle.common.util.constants.RunsConstants;
import net.neoforged.gradle.dsl.common.extensions.ProjectHolder;
import net.neoforged.gradle.dsl.common.runs.ide.extensions.IdeaRunExtension;
import net.neoforged.gradle.dsl.common.runs.run.Run;
import net.neoforged.gradle.dsl.common.util.CommonRuntimeUtils;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.plugins.ide.eclipse.model.EclipseModel;
import org.gradle.plugins.ide.idea.model.IdeaModel;
import org.jetbrains.gradle.ext.*;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

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
        final IdeManagementExtension ideManager = project.getExtensions().getByType(IdeManagementExtension.class);
        project.getExtensions().configure(RunsConstants.Extensions.RUNS, (Action<NamedDomainObjectContainer<Run>>) runs -> runs.configureEach(run -> {
            run.getExtensions().create(IdeaRunExtension.class, "idea", IdeaRunExtensionImpl.class, project, run);
        }));
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
                
                final TaskProvider<?> ideBeforeRunTask = project.getTasks().register(CommonRuntimeUtils.buildTaskName("ideBeforeRun", name), task -> {
                    for (SourceSet sourceSet : run.getModSources().get()) {
                        final Project sourceSetProject = sourceSet.getExtensions().getByType(ProjectHolder.class).getProject();
                        task.dependsOn(sourceSetProject.getTasks().named(sourceSet.getProcessResourcesTaskName()));
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
                
                ideaRuns.register(runName, Application.class, ideaRun -> {
                    runImpl.getWorkingDirectory().get().getAsFile().mkdirs();
                    
                    ideaRun.setMainClass(runImpl.getMainClass().get());
                    ideaRun.setWorkingDirectory(runImpl.getWorkingDirectory().get().getAsFile().getAbsolutePath());
                    ideaRun.setJvmArgs(String.join(" ", runImpl.realiseJvmArguments()));
                    ideaRun.moduleRef(project, runIdeaConfig.getPrimarySourceSet().get());
                    ideaRun.setProgramParameters(String.join(" ", runImpl.getProgramArguments().get()));
                    ideaRun.setEnvs(runImpl.getEnvironmentVariables().get());
                    
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
                    
                    final TaskProvider<?> ideBeforeRunTask = project.getTasks().register(CommonRuntimeUtils.buildTaskName("ideBeforeRun", name), task -> {
                        for (SourceSet sourceSet : run.getModSources().get()) {
                            final Project sourceSetProject = sourceSet.getExtensions().getByType(ProjectHolder.class).getProject();
                            task.dependsOn(sourceSetProject.getTasks().named(sourceSet.getProcessResourcesTaskName()));
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
                    
                    try {
                        final GradleLaunchConfig idePreRunTask = GradleLaunchConfig.builder(eclipse.getProject().getName())
                                                                         .tasks(ideBeforeRunTask.get().getName())
                                                                         .build();
                        
                        final String gradleName = ".Prepare " + runName;
                        writeLaunchToFile(project, gradleName, idePreRunTask);
                        
                        final JavaApplicationLaunchConfig debugRun =
                                JavaApplicationLaunchConfig.builder(eclipse.getProject().getName())
                                        .workingDirectory(runImpl.getWorkingDirectory().get().getAsFile().getAbsolutePath())
                                        .vmArgs(runImpl.realiseJvmArguments().toArray(new String[0]))
                                        .args(runImpl.getProgramArguments().get().toArray(new String[0]))
                                        .envVar(runImpl.getEnvironmentVariables().get())
                                        .build(runImpl.getMainClass().get());
                        
                        final String debugName = ".Run " + runName;
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
                                                       .action(LaunchGroup.Action.waitForTermination()))
                                        .build());
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to write launch files: " + runName, e);
                    }
                }));
            });
        }
        
        private static void writeLaunchToFile(Project project, String fileName, LaunchConfig config) {
            final File file = project.file(String.format("eclipse/runs/%s.launch", fileName));
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
    }
}
