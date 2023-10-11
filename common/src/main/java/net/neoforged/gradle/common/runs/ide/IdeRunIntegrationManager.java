package net.neoforged.gradle.common.runs.ide;

import net.neoforged.gradle.common.extensions.IdeManagementExtension;
import net.neoforged.gradle.common.runs.ide.extensions.IdeaRunExtensionImpl;
import net.neoforged.gradle.common.runs.run.RunImpl;
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
     * Configures the IDE integration to run runs as tasks from the IDE.
     *
     * @param project The project to configure.
     */
    public void apply(final Project project) {
        final IdeManagementExtension ideManager = project.getExtensions().getByType(IdeManagementExtension.class);
        ideManager.apply(new RegisterIdeRunExtensions());
        
        project.afterEvaluate(evaluatedProject -> {
            ideManager.apply(new RunsImportAction());
        });
    }

    private static final class RegisterIdeRunExtensions implements IdeManagementExtension.IdeImportAction {

        @Override
        public void idea(Project project, IdeaModel idea, ProjectSettings ideaExtension) {
            registerIdeaExtension(project);
        }
        
        private static void registerIdeaExtension(Project project) {
            project.getExtensions().configure(RunsConstants.Extensions.RUNS, (Action<NamedDomainObjectContainer<Run>>) runs -> runs.configureEach(run -> {
                run.getExtensions().create(IdeaRunExtension.class, "idea", IdeaRunExtensionImpl.class, project, run);
            }));
        }
        
        @Override
        public void eclipse(Project project, EclipseModel eclipse) {
            //TODO:
            // There is for now no native API, or library, yet which allows generating native
            // launch-files for eclipse without having to resort to unspecified launch arguments
            // when one becomes available we should implement that asap.
        }
        
        @Override
        public void gradle(Project project) {
            registerIdeaExtension(project);
            //TODO:
            // Implement eclipse registration if needed.
        }
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
            //TODO:
            // There is for now no native API, or library, yet which allows generating native
            // launch-files for eclipse without having to resort to unspecified launch arguments
            // when one becomes available we should implement that asap.
        }
    }
}
