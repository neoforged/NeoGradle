package net.minecraftforge.gradle.mcp.naming;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

@FunctionalInterface
public interface MinecraftVersionFromVersionDataProducer {
    /**
     * Gets the minecraft version from the version data.
     *
     * @param versionData The version data.
     * @return The minecraft version.
     */
    @NotNull
    String produce(Map<String, String> versionData);
}
