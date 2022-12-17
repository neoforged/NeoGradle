package net.minecraftforge.gradle.common.util;

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
        final String mappingVersion = mappingVersionData.get(NamingConstants.Version.VERSION);
        if (mappingVersion == null) {
            final String minecraftVersion = mappingVersionData.get(NamingConstants.Version.MINECRAFT_VERSION);
            if (minecraftVersion == null) {
                throw new IllegalStateException("Mapping version data does not contain a version or a minecraft version!");
            }

            return minecraftVersion;
        }
        return mappingVersion;
    }
}
