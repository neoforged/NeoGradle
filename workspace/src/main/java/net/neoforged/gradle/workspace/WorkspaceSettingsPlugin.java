package net.neoforged.gradle.workspace;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.initialization.ProjectDescriptor;
import org.gradle.api.initialization.Settings;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class WorkspaceSettingsPlugin implements Plugin<Settings> {
    @Override
    public void apply(Settings target) {
        target.getGradle().beforeProject(new DynamicProjectPluginAdapter(target));
    }

    private static final class DynamicProjectPluginAdapter implements Action<Project> {

        private final Settings settings;

        private DynamicProjectPluginAdapter(Settings settings) {
            this.settings = settings;
        }

        @Override
        public void execute(@NotNull Project project) {
            project.getLogger().lifecycle("Hello from WorkspacePlugin!");
        }
    }
}
