package net.neoforged.gradle.dsl.common.tasks.specifications

import net.neoforged.gdi.annotations.DSLProperty
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileTree
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile

import javax.inject.Inject

/**
 * Defines an object which has parameters related to output management.
 */
trait OutputSpecification implements ProjectSpecification {

    /**
     * The output file of this task as configured.
     * If not set, then it is derived from the output file name and the working directory of the task.
     *
     * @return The output file.
     */
    @DSLProperty
    @OutputFile
    @Optional
    abstract RegularFileProperty getOutput();

    /**
     * The name of the output file name for this task.
     * Can be left out, if and only if the output is set directly.
     *
     * @return The name of the output file.
     */
    @Input
    @Optional
    @DSLProperty
    abstract Property<String> getOutputFileName();

    /**
     * The output directory for this step, also doubles as working directory for this task.
     * Can be left out, if and only if the output is set directly.
     *
     * @return The output and working directory for this task.
     */
    @Internal
    @Optional
    @DSLProperty
    abstract DirectoryProperty getOutputDirectory();

    /**
     * The archive operator, allows access to zips, tars and their file trees.
     *
     * @return The archive operator.
     */
    @Inject
    abstract ArchiveOperations getArchiveOperations();

    /**
     * Creates a zip tree of the output.
     * If your output is not a zip tree, override this method with something else that matches.
     *
     * @return The file collection that represents the output. Might be a zip tree of the output jar, or a directory tree if it is an output directory. But it might also be a single file output collection.
     */
    @Internal
    Provider<? extends FileTree> getOutputAsTree() {
        return getOutput().map { it -> getArchiveOperations().zipTree(it) }
    }
}