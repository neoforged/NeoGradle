package net.minecraftforge.gradle.mcp.util;

import org.jetbrains.annotations.NotNull;

/**
 * Utility class for handling different mappings.
 */
public final class MappingUtils {

    private MappingUtils() {
        throw new IllegalStateException("MappingUtils is a utility class!");
    }

    @NotNull
    public static String buildMappingArtifact(final String channel, final String version) {
        return "%s_%s".formatted(channel, version);
    }
}
