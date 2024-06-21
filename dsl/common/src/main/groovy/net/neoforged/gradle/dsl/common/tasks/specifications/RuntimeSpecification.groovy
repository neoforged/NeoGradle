package net.neoforged.gradle.dsl.common.tasks.specifications

import net.minecraftforge.gdi.annotations.DSLProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input

/**
 * Defines a task that has a runtime specification.
 */
trait RuntimeSpecification {

    /**
     * The identifier of the step.
     * @return The identifier of the step.
     */
    @Input
    @DSLProperty
    abstract Property<String> getStep();
}