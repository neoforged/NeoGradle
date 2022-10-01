package net.minecraftforge.gradle.common.tasks;

import org.gradle.api.file.RegularFileProperty;

/**
 * Interface that indicates that a given task has an output which can be further processed.
 */
public interface ITaskWithOutput {

    /**
     * The output file of this task as configured.
     *
     * @return The output file.
     */
    RegularFileProperty getOutput();
}
