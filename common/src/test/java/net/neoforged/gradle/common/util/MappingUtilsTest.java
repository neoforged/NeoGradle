package net.neoforged.gradle.common.util;

import net.neoforged.gradle.dsl.common.util.NamingConstants;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class MappingUtilsTest {
    @Test
    public void mappingVersionIsFoundInMappingVersionData() {
        final Map<String, String> mappingVersionData = new HashMap<>();
        mappingVersionData.put(NamingConstants.Version.VERSION, "1.0.0");
        final String version = MappingUtils.getVersionOrMinecraftVersion(mappingVersionData);
        assertEquals("1.0.0", version);
    }

    @Test
    public void minecraftVersionIsFoundAssFallbackInMappingVersionData() {
        final Map<String, String> mappingVersionData = new HashMap<>();
        mappingVersionData.put(NamingConstants.Version.MINECRAFT_VERSION, "2.0.0");
        final String version = MappingUtils.getVersionOrMinecraftVersion(mappingVersionData);
        assertEquals("2.0.0", version);
    }

    @Test
    public void mappingVersionIsPrimarilyFoundInMappingVersionData() {
        final Map<String, String> mappingVersionData = new HashMap<>();
        mappingVersionData.put(NamingConstants.Version.VERSION, "1.0.0");
        mappingVersionData.put(NamingConstants.Version.MINECRAFT_VERSION, "2.0.0");
        final String version = MappingUtils.getVersionOrMinecraftVersion(mappingVersionData);
        assertEquals("1.0.0", version);
    }

    @Test
    public void throwsWhenNoVersionFoundInMappingVersionData() {
        final Map<String, String> mappingVersionData = new HashMap<>();
        assertThrows(IllegalStateException.class, () -> MappingUtils.getVersionOrMinecraftVersion(mappingVersionData));
    }

    @Test
    public void minecraftVersionIsFoundInMappingVersionData() {
        final Map<String, String> mappingVersionData = new HashMap<>();
        mappingVersionData.put(NamingConstants.Version.MINECRAFT_VERSION, "2.0.0");
        final String version = MappingUtils.getMinecraftVersion(mappingVersionData);
        assertEquals("2.0.0", version);
    }

    @Test
    public void throwsWhenNoMinecraftVersionFoundInMappingVersionData() {
        final Map<String, String> mappingVersionData = new HashMap<>();
        assertThrows(IllegalStateException.class, () -> MappingUtils.getMinecraftVersion(mappingVersionData));
    }
}
