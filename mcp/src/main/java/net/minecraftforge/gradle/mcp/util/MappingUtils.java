package net.minecraftforge.gradle.mcp.util;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Utility class for handling different mappings.
 */
public final class MappingUtils {

    private MappingUtils() {
        throw new IllegalStateException("MappingUtils is a utility class!");
    }


    @NotNull
    public static String getVersionOrMinecraftVersion(@NotNull Map<String, String> mappingVersionData) {
        final String mappingVersion = mappingVersionData.get(McpRuntimeConstants.Naming.Version.VERSION);
        if (mappingVersion == null) {
            final String minecraftVersion = mappingVersionData.get(McpRuntimeConstants.Naming.Version.MINECRAFT_VERSION);
            if (minecraftVersion == null) {
                throw new IllegalStateException("Mapping version data does not contain a version or a minecraft version!");
            }

            return minecraftVersion;
        }
        return mappingVersion;
    }
}
