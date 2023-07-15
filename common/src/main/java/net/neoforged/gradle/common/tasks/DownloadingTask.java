package net.neoforged.gradle.common.tasks;

import net.neoforged.gradle.dsl.common.extensions.ArtifactDownloader;
import net.neoforged.gradle.dsl.common.tasks.ForgeGradleBase;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Internal;

public abstract class DownloadingTask extends ForgeGradleBase {
    @Internal
    public final Provider<ArtifactDownloader> getDownloader() {
        return getProject().provider(() -> getProject().getExtensions().getByType(ArtifactDownloader.class));
    }

    public final Provider<String> getProjectFileName(final String postFix) {
        return getProject().provider(() -> String.format("%s_%s_%s", getProject().getName().replace(":", "_"), getName(), postFix));
    }
}
