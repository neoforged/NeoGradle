package net.neoforged.gradle.dsl.common.runtime.tasks.tree;

import net.neoforged.gradle.dsl.common.runtime.tasks.AsPartOfStep;
import org.gradle.api.Task;

import java.util.function.Consumer;

/**
 * Encapsulates a task customizer that changes the configuration of a Gradle Task encapsulating a Neoform step.
 */
public record TaskCustomizer<T extends Task & AsPartOfStep>(
    Class<T> taskClass,
    Consumer<T> taskCustomizer)
{
    public <F extends Task & AsPartOfStep> void apply(F task)
    {
        if (!taskClass.isInstance(task))
        {
            throw new IllegalArgumentException("Customization for step " + task.getStepName()
                + " requires task type " + taskClass + " but actual task is " + task.getClass());
        }
        taskCustomizer.accept(taskClass.cast(task));
    }
}
