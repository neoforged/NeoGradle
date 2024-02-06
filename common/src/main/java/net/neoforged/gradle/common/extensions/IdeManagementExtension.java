package net.neoforged.gradle.common.extensions;

import net.neoforged.gradle.common.tasks.IdePostSyncExecutionTask;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.plugins.ide.eclipse.EclipsePlugin;
import org.gradle.plugins.ide.eclipse.model.EclipseModel;
import org.gradle.plugins.ide.idea.model.IdeaModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.gradle.ext.IdeaExtPlugin;
import org.jetbrains.gradle.ext.ProjectSettings;
import org.jetbrains.gradle.ext.TaskTriggersConfig;

import java.io.File;
import java.nio.file.Files;
import java.util.function.BiConsumer;
import javax.inject.Inject;

/**
 * Defines interactions with the IDE plugins.
 */
public abstract class IdeManagementExtension {

    private final Project project;
    private final Project rootProject;

    @Inject
    public IdeManagementExtension(Project project) {
        this.project = project;
        this.rootProject = project.getRootProject();
        
        project.getPlugins().apply(IdeaExtPlugin.class);
        project.getPlugins().apply(EclipsePlugin.class);
        
        if (project != rootProject) {
            if (!rootProject.getPlugins().hasPlugin(IdeaExtPlugin.class))
                rootProject.getPlugins().apply(IdeaExtPlugin.class);
            
            if (!rootProject.getPlugins().hasPlugin(EclipsePlugin.class))
                rootProject.getPlugins().apply(EclipsePlugin.class);
        }

        // Always pre-create the idePostSync task if IntelliJ is importing the project, since
        // IntelliJ remembers to run the task post-sync even if the import fails. That will cause
        // situations where import errors (i.e. dependency resolution errors) will be masked by
        // the failed idePostSync task, since it was never created in that particular import.
        if (isIdeaImport()) {
            getOrCreateIdeImportTask();
        }
    }

    /**
     * Get whether Gradle is being invoked through IntelliJ IDEA.
     *
     * <p>This can be through a project import, or a task execution.</p>
     *
     * @return whether this is an IntelliJ-based invocation
     */
    public boolean isIdeaImport() {
        return Boolean.getBoolean("idea.active");
    }

    /**
     * Get whether this Gradle invocation is from an Eclipse project import.
     *
     * @return whether an eclipse import is ongoing
     */
    public boolean isEclipseImport() {
        return System.getProperty("eclipse.application") != null;
    }

    /**
     * @return true if should rather perform VsCode setup instead of Eclipse setup.
     * @implNote reinvestigate after https://github.com/microsoft/vscode-java-debug/issues/1106
     */
    private boolean isLikelyVscodeImport()
    {
        final boolean hasVscodeDirectory = Files.isDirectory(project.getRootDir().toPath().resolve(".vscode"));
        final boolean hasExtensionGradlePlugin = isDefinitelyVscodeImport(project);
        project.getLogger().debug("vscode detection - dir .vscode: " + hasVscodeDirectory);
        project.getLogger().debug("vscode detection - extension vscjava.vscode-gradle: " + hasExtensionGradlePlugin);
        return hasVscodeDirectory || hasExtensionGradlePlugin;
    }

    /**
     * @return true if must perform VsCode setup instead of Eclipse setup.
     */
    public static boolean isDefinitelyVscodeImport(final Project project)
    {
        final String pathToCheck = String.join(File.separator, new String[]{"data", "extensions", "vscjava.vscode-gradle"});
        final boolean hasUserDirProperty = System.getProperty("user.dir").contains(pathToCheck); 
        final boolean hasExtensionGradlePlugin = project.getPlugins().stream().anyMatch(p -> p.getClass().getName().equals("com.microsoft.gradle.GradlePlugin"));
        project.getLogger().debug("vscode detection - user.dir contains " + pathToCheck + ": " + hasUserDirProperty);
        project.getLogger().debug("vscode detection - extension vscjava.vscode-gradle: " + hasExtensionGradlePlugin);
        return hasUserDirProperty || hasExtensionGradlePlugin;
    }

    /**
     * Indicates if an IDE import in any of the supported IDEs is ongoing.
     *
     * @return {@code true} if an IDE import is ongoing, {@code false} otherwise
     */
    public boolean isIdeImportInProgress() {
        return isIdeaImport() || isEclipseImport() || isDefinitelyVscodeImport(project);
    }

    /**
     * Configures the current project to run a task after the IDE import is complete.
     *
     * @param taskToRun The task to run
     */
    public void registerTaskToRun(TaskProvider<?> taskToRun) {
        final TaskProvider<? extends Task> idePostSyncTask = getOrCreateIdeImportTask();
        //Configure the idePostSync task to depend on the task to run, causing the past in task to become part of the task-tree that is ran after import.
        idePostSyncTask.configure(task -> task.dependsOn(taskToRun));
    }
    
    @NotNull
    public TaskProvider<? extends Task> getOrCreateIdeImportTask() {
        final TaskProvider<? extends Task> idePostSyncTask;
        //Check for the existence of the idePostSync task, which is created by us as a central entry point for all IDE post-sync tasks
        if (!project.getTasks().getNames().contains("idePostSync")) {

            //None found -> Create one.
            idePostSyncTask = project.getTasks().register("idePostSync", IdePostSyncExecutionTask.class);

            //Register the task to run after the IDE import is complete
            apply(new IdeImportAction() {
                @Override
                public void idea(Project project, IdeaModel idea, ProjectSettings ideaExtension) {
                    //Register the task to run after the IDEA import is complete, via its custom extension.
                    final TaskTriggersConfig taskTriggers = ((ExtensionAware) ideaExtension).getExtensions().getByType(TaskTriggersConfig.class);
                    taskTriggers.afterSync(idePostSyncTask);
                }

                @Override
                public void eclipse(Project project, EclipseModel eclipse) {
                    //Register the task to run after the Eclipse import is complete, via its build-in support.
                    eclipse.synchronizationTasks(idePostSyncTask);
                }

                @Override
                public void vscode(Project project, EclipseModel eclipse) {
                    // vscode ~= eclipse
                    eclipse(project, eclipse);
                }
            });
        }
        else {
            //Found -> Use it.
            idePostSyncTask = project.getTasks().named("idePostSync");
        }
        return idePostSyncTask;
    }
    
    /**
     * Applies the specified configuration action to configure IDE projects.
     *
     * <p>This does not apply the IDEs' respective plugins, but will perform
     * actions when those plugins are applied.</p>
     *
     * @param toPerform the actions to perform
     */
    public void apply(final IdeImportAction toPerform) {
        onIdea(toPerform);
        // likely because we want to generate launch.json even if we don't have gradle extension
        if (isLikelyVscodeImport()) onVscode(toPerform);
        else onEclipse(toPerform);
        onGradle(toPerform);
    }
    
    /**
     * Applies the specified configuration action to configure idea IDE projects only.
     *
     * <p>This does not apply the idea IDEs' respective plugins, but will perform
     * actions when those plugins are applied.</p>
     *
     * @param toPerform the actions to perform
     */
    public void onIdea(final IdeaIdeImportAction toPerform) {
        //When the IDEA plugin is available, configure it
        project.getPlugins().withType(IdeaExtPlugin.class, plugin -> {
            if (!isIdeaImport()) {
                //No IDEA import even though the plugin is available, so don't configure it.
                return;
            }
            
            //Grab the idea runtime model so we can extend it. -> This is done from the root project, so that the model is available to all subprojects.
            //And so that post sync tasks are only ran once for all subprojects.
            IdeaModel model = project.getExtensions().findByType(IdeaModel.class);
            if (model == null || model.getProject() == null) {
                model = rootProject.getExtensions().findByType(IdeaModel.class);
            }
            
            //If we still don't have a model, throw an exception:
            if (model == null || model.getProject() == null) {
                throw new IllegalStateException("IDEA model is null, even though the IDEA plugin is applied.");
            }
            
            //Locate the project settings on the model and then configure them and then pass them to the action so that it can configure it.
            final ProjectSettings ideaExt = ((ExtensionAware) model.getProject()).getExtensions().getByType(ProjectSettings.class);
            
            //Configure the project, passing the model, extension, and the relevant project. Which does not need to be the root, but can be.
            toPerform.idea(project, model, ideaExt);
        });
    }

    public void onEclipse(final EclipseIdeImportAction toPerform) {
        onCommonEclipse(toPerform::eclipse);
    }

    public void onVscode(final VscodeIdeImportAction toPerform) {
        onCommonEclipse(toPerform::vscode);
    }

    /**
     * Applies the specified configuration action to configure eclipse IDE projects only.
     *
     * <p>This does not apply the eclipse IDEs' respective plugins, but will perform
     * actions when those plugins are applied.</p>
     *
     * @param toPerform the actions to perform
     */
    private void onCommonEclipse(final BiConsumer<Project, EclipseModel> toPerform) {
        //When the Eclipse plugin is available, configure it
        project.getPlugins().withType(EclipsePlugin.class, plugin -> {
            //Do not configure the eclipse plugin if we are not importing.
            if (!isEclipseImport() && !isDefinitelyVscodeImport(project)) {
                return;
            }
            
            //Grab the eclipse model so we can extend it. -> Done on the root project so that the model is available to all subprojects.
            //And so that post sync tasks are only ran once for all subprojects.
            EclipseModel model = project.getExtensions().findByType(EclipseModel.class);
            if (model == null) {
                model = rootProject.getExtensions().findByType(EclipseModel.class);
                if (model == null) {
                    return;
                }
            }
            
            //Configure the project, passing the model and the relevant project. Which does not need to be the root, but can be.
            toPerform.accept(project, model);
        });
    }
    
    /**
     * Applies the specified configuration action to configure gradle run projects only.
     *
     * @param toPerform the actions to perform
     */
    public void onGradle(final GradleIdeImportAction toPerform) {
        if (!isEclipseImport() && !isIdeaImport() && !isDefinitelyVscodeImport(project)) {
            toPerform.gradle(project);
        }
    }
    
    public interface GradleIdeImportAction {
        
        /**
         * Configure a gradle project.
         *
         * @param project the project being imported
         */
        default void gradle(Project project) {}
    }
    
    /**
     * A configuration action for idea IDE projects.
     */
    public interface IdeaIdeImportAction {
        
        /**
         * Configure an IntelliJ project.
         *
         * @param project       the project to configure on import
         * @param idea          the basic idea gradle extension
         * @param ideaExtension JetBrain's extensions to the base idea model
         */
        void idea(Project project, IdeaModel idea, ProjectSettings ideaExtension);
    }
    
    /**
     * A configuration action for eclipse IDE projects.
     */
    public interface EclipseIdeImportAction {
        
        /**
         * Configure an eclipse project.
         *
         * @param project the project being imported
         * @param eclipse the eclipse project model to modify
         */
        void eclipse(Project project, EclipseModel eclipse);
    }
    
    /**
     * A configuration action for vscode IDE projects.
     */
    public interface VscodeIdeImportAction {

        /**
         * Configure an vscode project.
         *
         * @param project the project being imported
         * @param eclipse the eclipse project model to modify
         */
        void vscode(Project project, EclipseModel eclipse);
    }
    
    /**
     * A configuration action for IDE projects.
     */
    public interface IdeImportAction extends IdeaIdeImportAction, EclipseIdeImportAction, VscodeIdeImportAction, GradleIdeImportAction { }
}
