package net.neoforged.gradle.util;

import org.gradle.api.Named;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a named record.
 * Default implements {@link Named} using {@link #name()}.
 */
public interface NamedRecord extends Named {

    @Override
    public default @NotNull String getName() {
        return name();
    }

    @NotNull
    String name();
}
