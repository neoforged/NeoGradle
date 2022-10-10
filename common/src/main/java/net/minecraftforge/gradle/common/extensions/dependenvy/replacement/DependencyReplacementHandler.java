package net.minecraftforge.gradle.common.extensions.dependenvy.replacement;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@FunctionalInterface
public interface DependencyReplacementHandler {

    @NotNull
    Optional<DependencyReplacementResult> get(@NotNull DependencyReplacementContext context);
}
