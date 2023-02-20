package net.minecraftforge.gradle.base.util;

import net.minecraftforge.gradle.dsl.base.util.NamingConstants;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MappingUtilsTest {

    @Test
    void mappingVersionIsFoundInMappingVersionData() {
        final Map<String, String> mappingVersionData = new HashMap<>();
        mappingVersionData.put(NamingConstants.Version.VERSION, "1.0.0");
        final String version = MappingUtils.getVersionOrMinecraftVersion(mappingVersionData);
        assertEquals("1.0.0", version);
    }

    @Test
    void minecraftVersionIsFoundAssFallbackInMappingVersionData() {
        final Map<String, String> mappingVersionData = new HashMap<>();
        mappingVersionData.put(NamingConstants.Version.MINECRAFT_VERSION, "2.0.0");
        final String version = MappingUtils.getVersionOrMinecraftVersion(mappingVersionData);
        assertEquals("2.0.0", version);
    }

    @Test
    void mappingVersionIsPrimarilyFoundInMappingVersionData() {
        final Map<String, String> mappingVersionData = new HashMap<>();
        mappingVersionData.put(NamingConstants.Version.VERSION, "1.0.0");
        mappingVersionData.put(NamingConstants.Version.MINECRAFT_VERSION, "2.0.0");
        final String version = MappingUtils.getVersionOrMinecraftVersion(mappingVersionData);
        assertEquals("1.0.0", version);
    }

    @Test
    void throwsWhenNoVersionFoundInMappingVersionData() {
        final Map<String, String> mappingVersionData = new HashMap<>();
        assertThrows(IllegalStateException.class, () -> MappingUtils.getVersionOrMinecraftVersion(mappingVersionData));
    }

    @Test
    void minecraftVersionIsFoundInMappingVersionData() {
        final Map<String, String> mappingVersionData = new HashMap<>();
        mappingVersionData.put(NamingConstants.Version.MINECRAFT_VERSION, "2.0.0");
        final String version = MappingUtils.getMinecraftVersion(mappingVersionData);
        assertEquals("2.0.0", version);
    }

    @Test
    void throwsWhenNoMinecraftVersionFoundInMappingVersionData() {
        final Map<String, String> mappingVersionData = new HashMap<>();
        assertThrows(IllegalStateException.class, () -> MappingUtils.getMinecraftVersion(mappingVersionData));
    }
}