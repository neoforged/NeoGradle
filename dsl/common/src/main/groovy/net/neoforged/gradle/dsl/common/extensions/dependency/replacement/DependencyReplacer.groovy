package net.neoforged.gradle.dsl.common.extensions.dependency.replacement

import groovy.transform.CompileStatic
import org.jetbrains.annotations.NotNull

/**
 * Defines a dependency replacer.
 * A dependency replacer is a function which takes a dependency and optionally replaces it with another dependency.
 */
@CompileStatic
@FunctionalInterface
interface DependencyReplacer {

    /**
     * Invoked to check if this replacer can replace the given dependency.
     *
     * @param context The context of the dependency replacement.
     * @return An optional, potentially containing a replacement for the dependency.
     */
    @NotNull
    Optional<DependencyReplacementResult> get(@NotNull Context context);
}
