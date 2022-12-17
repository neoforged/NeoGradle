package net.minecraftforge.gradle.common.runtime.naming;

import net.minecraftforge.gradle.common.runtime.tasks.IRuntimeTask;
import net.minecraftforge.gradle.common.tasks.ITaskWithOutput;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;

/**
 * Factory interface which can construct a new task provider for a compiled jar mapping.
 */
@FunctionalInterface
public interface ApplyMappingsToCompiledJarTaskBuilder {

    /**
     * Invoked to construct a new task provider for a compiled jar mapping from the given context.
     *
     * @param context The context.
     * @return The task provider.
     */
    @NotNull TaskProvider<? extends ITaskWithOutput> build(ApplyMappingsTaskBuildingContext context);
}
