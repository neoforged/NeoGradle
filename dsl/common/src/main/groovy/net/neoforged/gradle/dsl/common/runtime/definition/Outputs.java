package net.neoforged.gradle.dsl.common.runtime.definition;

import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import org.gradle.api.tasks.TaskProvider;

/**
 * Represents the outputs of a runtime.
 */
public interface Outputs {

    /**
     * @return The outputs instance that indicates that neither the sources nor binaries are known.
     */
    static Outputs unknown() {
        return new Outputs() {
            @Override
            public TaskProvider<? extends WithOutput> sources() {
                throw new IllegalStateException("The sources output is unknown");
            }

            @Override
            public TaskProvider<? extends WithOutput> binaries() {
                throw new IllegalStateException("The binaries output is unknown");
            }
        };
    }

    /**
     * @return The task that produces the sources jar of the runtime.
     */
    TaskProvider<? extends WithOutput> sources();

    /**
     * @return The task that produces the binary jar of the runtime.
     */
    TaskProvider<? extends WithOutput> binaries();
}
