package net.minecraftforge.gradle.common.tasks;

import org.gradle.api.Task;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;

/**
 * Interface that indicates that a given task has an output which can be further processed.
 */
public interface ITaskWithOutput extends Task {

    /**
     * The output file of this task as configured.
     *
     * @return The output file.
     */
    @OutputFile
    RegularFileProperty getOutput();

    /**
     * The name of the output file name for this step.
     *
     * @return The name of the output file.
     */
    @Input
    Property<String> getOutputFileName();
}
