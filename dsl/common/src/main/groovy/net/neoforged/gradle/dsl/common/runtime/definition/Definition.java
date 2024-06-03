package net.neoforged.gradle.dsl.common.runtime.definition;

import net.neoforged.gradle.dsl.common.runtime.spec.Specification;
import net.neoforged.gradle.util.NamedRecord;
import org.gradle.api.artifacts.Configuration;
import org.jetbrains.annotations.NotNull;

public record Definition(
        Specification specification,
        Configuration dependencies,
        TaskHandler taskHandler
) implements NamedRecord {

    @Override
    public @NotNull String name() {
        return specification.name();
    }
}
