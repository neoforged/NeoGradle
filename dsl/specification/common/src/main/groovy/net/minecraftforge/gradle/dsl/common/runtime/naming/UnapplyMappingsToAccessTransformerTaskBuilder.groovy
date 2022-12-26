package net.minecraftforge.gradle.dsl.common.runtime.naming

import groovy.transform.CompileStatic
import net.minecraftforge.gradle.dsl.common.runtime.tasks.Runtime
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.annotations.NotNull

/**
 * Factory interface which can construct a new task provider for a access transformer unmapping.
 */
@CompileStatic
@FunctionalInterface
interface UnapplyMappingsToAccessTransformerTaskBuilder {

    /**
     * Invoked to construct a new task provider for a access transformer unmapping from the given context.
     *
     * @param context The unmapping task building context.
     * @return The task provider.
     */
    @NotNull
    TaskProvider<? extends Runtime> apply(final TaskBuildingContext context)
}