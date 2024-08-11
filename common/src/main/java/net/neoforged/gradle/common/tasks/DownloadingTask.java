package net.neoforged.gradle.common.tasks;

import net.neoforged.gradle.dsl.common.tasks.NeoGradleBase;

public abstract class DownloadingTask extends NeoGradleBase {
    
    public final String getProjectFileName(final String postFix) {
        return String.format("%s_%s_%s", getProject().getName().replace(":", "_"), getName(), postFix);
    }
}
