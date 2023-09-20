package net.neoforged.gradle.dsl.common.tasks

import groovy.transform.CompileStatic
import net.minecraftforge.gdi.annotations.DefaultMethods;
import org.gradle.api.Task
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.FileSystemOperations;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Provider

import javax.inject.Inject

/**
 * Defines a task which has a workspace associated with it.
 * The workspace is a directory where the task can store files.
 *
 * This interface provides methods for working with this workspace.
 */
@CompileStatic
@DefaultMethods
trait WithWorkspace extends WithOperations {

    /**
     * Ensures the workspace is ready for the file and realises the given property and regular file.
     *
     * @param fileProvider The provider for the file.
     * @return The file.
     */
    default File ensureFileWorkspaceReady(final RegularFileProperty fileProvider) {
        return ensureFileWorkspaceReady(fileProvider.getAsFile());
    }

    /**
     * Ensures the workspace is ready for the file
     *
     * @param regularFile The file.
     * @return The file as IO File.
     */
    default File ensureFileWorkspaceReady(final RegularFile regularFile) {
        return ensureFileWorkspaceReady(regularFile.getAsFile());
    }

    /**
     * Ensures the workspace is ready for the file and realises the given provider.
     *
     * @param fileProvider The provider for the file.
     * @return The file as IO File.
     */
    default File ensureFileWorkspaceReady(final Provider<File> fileProvider) {
        return ensureFileWorkspaceReady(fileProvider.get());
    }

    /**
     * Works as a provider transformer to ensure the file is ready when needed without realising it at the time of calling.
     *
     * @param fileProvider The provider for the file.
     * @return The provider for the file, rigged for realisation.
     */
    default Provider<File> transformEnsureFileWorkspaceReady(final Provider<File> fileProvider) {
        return fileProvider.map(this::ensureFileWorkspaceReady);
    }

    /**
     * Works as a provider transformer to ensure the file is ready when needed without realising it at the time of calling.
     *
     * @param fileProvider The provider for the file.
     * @return The provider for the file, rigged for realisation.
     */
    default Provider<File> transformEnsureFileWorkspaceReady(final File fileProvider) {
        return getProviderFactory().provider(() -> fileProvider).map(this::ensureFileWorkspaceReady);
    }

    /**
     * Ensures the workspace is ready for the file.
     *
     * @param file The file.
     * @return The file as IO File.
     */
    default File ensureFileWorkspaceReady(final File file) {
        if (file.exists()) {
            file.delete();
            return file;
        }

        file.getParentFile().mkdirs();
        return file;
    }

    /**
     * Creates a new provider for the given value.
     *
     * @param value The value.
     * @return The provider.
     */
    default <T> Provider<T> newProvider(final T value) {
        return getProviderFactory().provider(() -> value);
    }
}
