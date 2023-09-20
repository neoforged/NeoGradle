package net.neoforged.gradle.dsl.common.tasks

import org.gradle.api.Task
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ProviderFactory
import org.gradle.process.ExecOperations

import javax.inject.Inject

/**
 * Defines a base task type that has access to injected operations classes to deal with configuration caching.
 */
trait WithOperations implements Task {

    /**
     * The provider factory to create new temporary chainable providers.
     *
     * @return The provider factory.
     */
    @Inject
    abstract ProviderFactory getProviderFactory();

    /**
     * The object factory that can be used to manage the internal subsystems of a gradle model.
     * Allows for the creation of for example file collections, trees and other components.
     *
     * @return The object factory.
     */
    @Inject
    abstract ObjectFactory getObjectFactory();

    /**
     * The execution operator, allows for the execution of external tools.
     *
     * @return The execution operator.
     */
    @Inject
    abstract ExecOperations getExecuteOperation();

    /**
     * The archive operator, allows access to zips, tars and their file trees.
     *
     * @return The archive operator.
     */
    @Inject
    abstract ArchiveOperations getArchiveOperations();

    /**
     * The file system operator, allows access to operations related to file system interactions,
     * for example copying, syncing and deleting.
     *
     * @return The file system operator.
     */
    @Inject
    abstract FileSystemOperations getFileSystemOperations();

    /**
     * Gives access to the layout that the current project has without a reference directly to the project.
     *
     * @return The layout of the project.
     */
    @Inject
    abstract ProjectLayout getLayout();
}