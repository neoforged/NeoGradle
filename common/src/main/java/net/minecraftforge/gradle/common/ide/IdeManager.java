package net.minecraftforge.gradle.common.ide;

import net.minecraftforge.gradle.common.ide.task.IdePostSyncExecutionTask;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.plugins.ide.eclipse.EclipsePlugin;
import org.gradle.plugins.ide.eclipse.model.EclipseModel;
import org.gradle.plugins.ide.idea.model.IdeaModel;
import org.jetbrains.gradle.ext.IdeaExtPlugin;
import org.jetbrains.gradle.ext.ProjectSettings;
import org.jetbrains.gradle.ext.TaskTriggersConfig;

public final class IdeManager {
    private static final IdeManager INSTANCE = new IdeManager();

    public static IdeManager getInstance() {
        return INSTANCE;
    }

    private IdeManager() {
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
     * Indicates if an IDE import in any of the supported IDEs is ongoing.
     *
     * @return {@code true} if an IDE import is ongoing, {@code false} otherwise
     */
    public boolean isIdeImportInProgress() {
        return isIdeaImport() || isEclipseImport();
    }

    public void registerTaskToRun(Project project, TaskProvider<?> taskToRun) {
        final TaskProvider<? extends Task> idePostSyncTask;
        if (project.getTasks().findByName("idePostSync") == null) {
            idePostSyncTask = project.getTasks().register("idePostSync", IdePostSyncExecutionTask.class);

            apply(project, new IdeImportAction() {
                @Override
                public void idea(Project project, IdeaModel idea, ProjectSettings ideaExtension) {
                    // Navigate via the extension properties...
                    // https://github.com/JetBrains/gradle-idea-ext-plugin/wiki
                    final TaskTriggersConfig taskTriggers = ((ExtensionAware) ideaExtension).getExtensions().getByType(TaskTriggersConfig.class);
                    taskTriggers.afterSync(idePostSyncTask);
                }

                @Override
                public void eclipse(Project project, EclipseModel eclipse) {
                    eclipse.synchronizationTasks(idePostSyncTask);
                }
            });
        }
        else {
            idePostSyncTask = project.getTasks().named("idePostSync");
        }

        idePostSyncTask.configure(task -> task.dependsOn(taskToRun));
    }

    /**
     * Applies the specified configuration action to configure IDE projects.
     *
     * <p>This does not apply the IDEs' respective plugins, but will perform
     * actions when those plugins are applied.</p>
     *
     * @param project project to apply to
     * @param toPerform the actions to perform
     */
    public void apply(final Project project, final IdeImportAction toPerform) {
        project.getPlugins().withType(IdeaExtPlugin.class, plugin -> {
            if (!isIdeaImport()) {
                return;
            }

            // Apply the IDE plugin to the root project
            final Project rootProject = project.getRootProject();
            if (project != rootProject) {
                rootProject.getPlugins().apply(IdeaExtPlugin.class);
            }
            final IdeaModel model = rootProject.getExtensions().findByType(IdeaModel.class);
            if (model == null || model.getProject() == null) {
                return;
            }
            final ProjectSettings ideaExt = ((ExtensionAware) model.getProject()).getExtensions().getByType(ProjectSettings.class);

            // But actually perform the configuration with the subproject context
            toPerform.idea(project, model, ideaExt);
        });
        project.getPlugins().withType(EclipsePlugin.class, plugin -> {
            final EclipseModel model = project.getExtensions().findByType(EclipseModel.class);
            if (model == null) {
                return;
            }
            toPerform.eclipse(project, model);
        });
    }

    public interface IdeImportAction {

        /**
         * Configure an IntelliJ project.
         *
         * @param project the project to configure on import
         * @param idea the basic idea gradle extension
         * @param ideaExtension JetBrain's extensions to the base idea model
         */
        void idea(final Project project, final IdeaModel idea, final ProjectSettings ideaExtension);

        /**
         * Configure an eclipse project.
         *
         * @param project the project being imported
         * @param eclipse the eclipse project model to modify
         */
        void eclipse(final Project project, final EclipseModel eclipse);

    }
}
