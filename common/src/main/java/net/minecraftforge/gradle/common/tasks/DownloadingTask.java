package net.minecraftforge.gradle.common.tasks;

import net.minecraftforge.gradle.common.extensions.ArtifactDownloaderExtension;
import net.minecraftforge.gradle.dsl.common.extensions.ArtifactDownloader;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Internal;

public abstract class DownloadingTask extends ForgeGradleBaseTask {
    @Internal
    public final Provider<ArtifactDownloader> getDownloader() {
        return getProject().provider(() -> getProject().getExtensions().getByType(ArtifactDownloader.class));
    }

    public final Provider<String> getProjectFileName(final String postFix) {
        return getProject().provider(() -> String.format("%s_%s_%s", getProject().getName().replace(":", "_"), getName(), postFix));
    }
}
