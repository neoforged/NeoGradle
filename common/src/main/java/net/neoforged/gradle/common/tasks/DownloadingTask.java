package net.neoforged.gradle.common.tasks;

import net.neoforged.gradle.dsl.common.extensions.ArtifactDownloader;
import net.neoforged.gradle.dsl.common.tasks.NeoGradleBase;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;

public abstract class DownloadingTask extends NeoGradleBase {
    
    public final String getProjectFileName(final String postFix) {
        return String.format("%s_%s_%s", getProject().getName().replace(":", "_"), getName(), postFix);
    }
}
