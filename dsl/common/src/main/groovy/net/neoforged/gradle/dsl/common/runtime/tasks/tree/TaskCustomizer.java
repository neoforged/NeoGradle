package net.neoforged.gradle.dsl.common.runtime.tasks.tree;

import net.neoforged.gradle.dsl.common.runtime.tasks.Runtime;
import org.gradle.api.Task;

import java.util.function.Consumer;

/**
 * Encapsulates a task customizer that changes the configuration of a Gradle Task encapsulating a Neoform step.
 */
public final class TaskCustomizer<T extends Task> {
    private final Class<T> taskClass;
    private final Consumer<T> taskCustomizer;

    public TaskCustomizer(Class<T> taskClass, Consumer<T> taskCustomizer) {
        this.taskClass = taskClass;
        this.taskCustomizer = taskCustomizer;
    }

    /**
     * @return The expected task type. This will be validated to avoid unchecked casts.
     */
    public Class<T> getTaskClass() {
        return taskClass;
    }

    /**
     * @return The function that will be applied to the task using {@link Task#configure}.
     */
    public Consumer<T> getTaskCustomizer() {
        return taskCustomizer;
    }

    public void apply(Runtime task) {
        if (!taskClass.isInstance(task)) {
            throw new IllegalArgumentException("Customization for step " + task.getStep()
                    + " requires task type " + taskClass + " but actual task is " + task.getClass());
        }
        taskCustomizer.accept(taskClass.cast(task));
    }
}
