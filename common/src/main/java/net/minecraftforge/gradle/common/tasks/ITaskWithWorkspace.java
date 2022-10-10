package net.minecraftforge.gradle.common.tasks;

import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Provider;

import java.io.File;

public interface ITaskWithWorkspace {
    default File ensureFileWorkspaceReady(final RegularFileProperty fileProvider) {
        return ensureFileWorkspaceReady(fileProvider.getAsFile());
    }

    default Provider<File> transformEnsureFileWorkspaceReady(final RegularFileProperty fileProvider) {
        return fileProvider.map(this::ensureFileWorkspaceReady);
    }

    default File ensureFileWorkspaceReady(final RegularFile fileProvider) {
        return ensureFileWorkspaceReady(fileProvider.getAsFile());
    }

    default File ensureFileWorkspaceReady(final Provider<File> fileProvider) {
        return ensureFileWorkspaceReady(fileProvider.get());
    }

    default Provider<File> transformEnsureFileWorkspaceReady(final Provider<File> fileProvider) {
        return fileProvider.map(this::ensureFileWorkspaceReady);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    default File ensureFileWorkspaceReady(final File file) {
        if (file.exists()) {
            file.delete();
            return file;
        }

        file.getParentFile().mkdirs();
        return file;
    }
}
