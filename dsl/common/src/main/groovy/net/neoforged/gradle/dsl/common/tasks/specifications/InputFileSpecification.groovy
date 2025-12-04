package net.neoforged.gradle.dsl.common.tasks.specifications

import net.neoforged.gdi.annotations.DSLProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity

/**
 * Represents an object with an input file.
 */
trait InputFileSpecification implements ProjectSpecification {

    /**
     * The input file of this task as configured.
     * If not set, then it is derived from the input file name and the working directory of the task.
     *
     * @return The input file.
     */
    @DSLProperty
    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.NONE)
    abstract RegularFileProperty getInput();
}