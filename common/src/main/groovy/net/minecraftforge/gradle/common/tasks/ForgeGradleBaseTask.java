package net.minecraftforge.gradle.common.tasks;

import org.gradle.api.DefaultTask;
import org.gradle.api.logging.LogLevel;

public abstract class ForgeGradleBaseTask extends DefaultTask implements ITaskWithWorkspace {

    public ForgeGradleBaseTask() {
        setGroup("ForgeGradle");

        getLogging().captureStandardOutput(LogLevel.DEBUG);
        getLogging().captureStandardError(LogLevel.ERROR);
    }
}
