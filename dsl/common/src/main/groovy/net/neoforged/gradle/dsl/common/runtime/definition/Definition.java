package net.neoforged.gradle.dsl.common.runtime.definition;

import net.neoforged.gradle.dsl.common.runtime.spec.Specification;
import net.neoforged.gradle.util.NamedRecord;
import org.jetbrains.annotations.NotNull;

public record Definition(
        Specification specification,
        DependencyHandler dependencies,
        TaskHandler taskHandler,
        Outputs outputs
) implements NamedRecord {

    @Override
    public @NotNull String name() {
        return specification.identifier();
    }
}
