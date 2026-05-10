package net.neoforged.gradle.dsl.common.runtime.tasks

import net.neoforged.gdi.annotations.DSLProperty
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal

trait AsPartOfStep {

    /**
     * The runtime directory, it is the location of the runtime working directory, inside of which the step resides.
     *
     * @return The runtime working directory.
     */
    @Internal
    abstract DirectoryProperty getRuntimeDirectory();

    /**
     * The steps directory, it is the location of the steps working directory.
     *
     * @return The steps directory.
     */
    @Internal
    abstract DirectoryProperty getStepsDirectory();

    /**
     * The name of the step.
     * @return The name of the step.
     */
    @Input
    @DSLProperty
    abstract Property<String> getStepName();
}