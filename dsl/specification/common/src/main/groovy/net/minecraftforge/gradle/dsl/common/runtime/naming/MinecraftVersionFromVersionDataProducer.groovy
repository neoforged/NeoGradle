package net.minecraftforge.gradle.dsl.common.runtime.naming

import groovy.transform.CompileStatic
import org.jetbrains.annotations.NotNull

/**
 * Defines a function which can produce a Minecraft version from a given mapping version data.
 */
@FunctionalInterface
@CompileStatic
interface MinecraftVersionFromVersionDataProducer {

    /**
     * Gets the minecraft version from the version data.
     *
     * @param versionData The version data.
     * @return The minecraft version.
     */
    @NotNull
    String produce(Map<String, String> versionData);
}
