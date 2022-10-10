package net.minecraftforge.gradle.common.extensions.dependenvy.replacement;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.jetbrains.annotations.NotNull;

public record DependencyReplacementContext(
        @NotNull Project project,
        @NotNull Configuration configuration,
        @NotNull Dependency dependency
        ) {
}
