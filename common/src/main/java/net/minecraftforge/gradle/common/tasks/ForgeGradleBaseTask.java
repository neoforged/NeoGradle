package net.minecraftforge.gradle.common.tasks;

import net.minecraftforge.gradle.common.util.TransformerUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Provider;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public abstract class ForgeGradleBaseTask extends DefaultTask {

    public ForgeGradleBaseTask() {
        setGroup("Forge Gradle");
    }

    protected File ensureFileWorkspaceReady(final RegularFileProperty fileProvider) {
        return ensureFileWorkspaceReady(fileProvider.getAsFile());
    }

    protected Provider<File> transformEnsureFileWorkspaceReady(final RegularFileProperty fileProvider) {
        return fileProvider.map(this::ensureFileWorkspaceReady);
    }

    protected File ensureFileWorkspaceReady(final RegularFile fileProvider) {
        return ensureFileWorkspaceReady(fileProvider.getAsFile());
    }

    protected File ensureFileWorkspaceReady(final Provider<File> fileProvider) {
        return ensureFileWorkspaceReady(fileProvider.get());
    }

    protected Provider<File> transformEnsureFileWorkspaceReady(final Provider<File> fileProvider) {
        return fileProvider.map(this::ensureFileWorkspaceReady);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    protected File ensureFileWorkspaceReady(final File file) {
        if (file.exists()) {
            file.delete();
            return file;
        }

        file.getParentFile().mkdirs();
        return file;
    }


}
