package net.neoforged.gradle.util;

import org.gradle.api.Named;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a named record.
 * Default implements {@link Named} using {@link #name()}.
 */
public interface NamedRecord extends Named {

    /**
     * @return The name of the record.
     */
    @Override
    default @NotNull String getName() {
        return name();
    }

    /**
     * @return A record friendly version of {@link Named#getName()}
     */
    @NotNull
    String name();
}
