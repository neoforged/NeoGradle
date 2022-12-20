package net.minecraftforge.gradle.dsl.common.runtime.naming

import groovy.transform.CompileStatic
import net.minecraftforge.gradle.dsl.common.tasks.WithOutput
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.annotations.NotNull

/**
 * Factory interface which can construct a new task provider for a compiled jar unmapping.
 */
@CompileStatic
@FunctionalInterface
interface UnapplyMappingsToCompiledJarTaskBuilder {

    /**
     * Invoked to construct a new task provider for a compiled jar unmapping from the given context.
     *
     * @param context The context.
     * @return The task provider.
     */
    @NotNull
    TaskProvider<? extends WithOutput> apply(final TaskBuildingContext context);
}
