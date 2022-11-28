package net.minecraftforge.gradle.common.runtime.naming;

import net.minecraftforge.gradle.common.tasks.ITaskWithOutput;
import org.gradle.api.tasks.TaskProvider;

@FunctionalInterface
public interface UnapplyMappingsToCompiledJarTaskBuilder {

    public TaskProvider<? extends ITaskWithOutput> apply(final UnapplyMappingsTaskBuildingContext context);
}
