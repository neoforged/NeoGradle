package net.neoforged.gradle.workspace;

import org.gradle.api.Plugin;
import org.gradle.api.initialization.Settings;

public class WorkspacePlugin implements Plugin<Object> {
    @Override
    public void apply(Object target) {
        if (target instanceof Settings) {
           Settings settings = (Settings) target;
           settings.getPlugins().apply(WorkspaceSettingsPlugin.class);
        }
    }
}
