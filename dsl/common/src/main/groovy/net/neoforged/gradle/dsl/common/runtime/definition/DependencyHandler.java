package net.neoforged.gradle.dsl.common.runtime.definition;

import org.gradle.api.artifacts.Configuration;

/**
 * Handles the configurations of a runtime.
 *
 * @param apiElements The API elements configuration that contains the API of the runtime.
 * @param runtimeElements The runtime elements configuration that contains the runtime of the runtime.
 */
public record DependencyHandler(
        Configuration apiElements,
        Configuration runtimeElements
) {
}
