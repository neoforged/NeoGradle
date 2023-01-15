package net.minecraftforge.gradle.dsl.common.tasks.specifications

import net.minecraftforge.gradle.dsl.annotations.DSLProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile

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
    abstract RegularFileProperty getOutput();

    /**
     * The name of the output file name for this step.
     * Can be left out, if and only if the output is set directly.
     *
     * @return The name of the output file.
     */
    @Input
    @Optional
    @DSLProperty
    abstract Property<String> getOutputFileName();
}