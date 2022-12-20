package net.minecraftforge.gradle.dsl.common.tasks

import net.minecraftforge.gradle.dsl.annotations.DSLProperty;
import org.gradle.api.Task;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;

/**
 * Interface that indicates that a given task has an output which can be further processed.
 */
interface WithOutput extends Task, WithWorkspace {

    /**
     * The output file of this task as configured.
     * If not set, then it is derived from the output file name and the working directory of the task.
     *
     * @return The output file.
     */
    @DSLProperty
    @OutputFile
    RegularFileProperty getOutput();

    /**
     * The name of the output file name for this step.
     * Can be left out, if and only if the output is set directly.
     *
     * @return The name of the output file.
     */
    @Input
    @Optional
    @DSLProperty
    Property<String> getOutputFileName();
}
