package net.minecraftforge.gradle.common.util;

import net.minecraftforge.gradle.dsl.common.util.NamingConstants;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class MappingUtils {
    private MappingUtils() {
        throw new IllegalStateException("MappingUtils is a utility class!");
    }

    /**
     * Gets the version of the mappings in use from the version data map.
     * Falling back to the minecraft version if the version is not present.
     *
     * @param mappingVersionData The version data map.
     * @return The version of the mappings in use.
     * @see NamingConstants.Version#VERSION
     * @see NamingConstants.Version#MINECRAFT_VERSION
     */
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

    /**
     * Gets the minecraft version of the mappings in use from the version data map.
     *
     * @param mappingVersionData The version data map.
     * @return The minecraft version of the mappings in use.
     * @see NamingConstants.Version#MINECRAFT_VERSION
     */
    @NotNull
    public static String getMinecraftVersion(@NotNull Map<String, String> mappingVersionData) {
        final String minecraftVersion = mappingVersionData.get(NamingConstants.Version.MINECRAFT_VERSION);
        if (minecraftVersion == null) {
            throw new IllegalStateException("Mapping version data does not contain a version or a minecraft version!");
        }

        return minecraftVersion;
    }
}
