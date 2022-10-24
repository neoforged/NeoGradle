package net.minecraftforge.gradle.common.tasks;

import net.minecraftforge.gradle.common.extensions.ArtifactDownloaderExtension;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Internal;

public abstract class DownloadingTask extends ForgeGradleBaseTask {
    @Internal
    public final Provider<ArtifactDownloaderExtension> getDownloader() {
        return getProject().provider(() -> getProject().getExtensions().getByType(ArtifactDownloaderExtension.class));
    }

    public final Provider<String> getProjectFileName(final String postFix) {
        return getProject().provider(() -> String.format("%s_%s_%s", getProject().getName().replace(":", "_"), getName(), postFix));
    }
}
