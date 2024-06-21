package net.neoforged.gradle.dsl.common.tasks.specifications

import net.minecraftforge.gdi.annotations.DSLProperty
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile

/**
 * Defines an object which has parameters related to output management.
 */
trait OutputSpecification implements ProjectSpecification {

    /**
     * The output directory of this task as configured.
     * If not set, then it is derived from the output directory identifier and the working directory of the task.
     *
     * @return The output directory.
     */
    @DSLProperty
    @OutputDirectory
    @Optional
    abstract DirectoryProperty getOutputDirectory();

    /**
     * The output file of this task as configured.
     * If not set, then it is derived from the output file identifier and the working directory of the task.
     *
     * @return The output file.
     */
    @DSLProperty
    @OutputFile
    @Optional
    abstract RegularFileProperty getOutput();

    /**
     * The identifier of the output file identifier for this step.
     * Can be left out, if and only if the output is set directly.
     *
     * @return The identifier of the output file.
     */
    @Input
    @Optional
    @DSLProperty
    abstract Property<String> getOutputFileName();
}