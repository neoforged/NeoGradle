package net.minecraftforge.gradle.common.extensions;

import org.gradle.api.initialization.Settings;

public abstract class SettingsExtension {

    private final Settings settings;
    private boolean setupLocalBuildCache = true;

    protected SettingsExtension(Settings settings) {
        this.settings = settings;
    }

    public Settings getSettings() {
        return settings;
    }

    public boolean isSetupLocalBuildCache() {
        return setupLocalBuildCache;
    }

    public void setSetupLocalBuildCache(boolean setupLocalBuildCache) {
        this.setupLocalBuildCache = setupLocalBuildCache;
    }
}
