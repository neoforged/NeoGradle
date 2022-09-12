package net.minecraftforge.gradle.common.tasks;

import net.minecraftforge.gradle.common.extensions.ArtifactDownloaderExtension;
import org.gradle.api.DefaultTask;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Internal;

import java.io.File;

public abstract class DownloadingTask extends DefaultTask {
    @Internal
    public final Provider<ArtifactDownloaderExtension> getDownloader() {
        return getProject().provider(() -> getProject().getExtensions().getByType(ArtifactDownloaderExtension.class));
    }

    @Internal
    public final Provider<String> getProjectFileName(final String postFix) {
        return getProject().provider(() -> "%s_%s_%s".formatted(getProject().getName().replace(":", "_"), getName(), postFix));
    }
}
