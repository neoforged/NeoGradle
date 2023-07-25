package net.neoforged.gradle.common.runtime.naming;

import net.neoforged.gradle.common.util.MappingUtils;
import net.neoforged.gradle.dsl.common.runtime.naming.DependencyNotationVersionManager;
import net.neoforged.gradle.dsl.common.util.NamingConstants;

import java.util.HashMap;
import java.util.Map;

public class SimpleDependencyNotationVersionManager implements DependencyNotationVersionManager {

    private final String key;

    public SimpleDependencyNotationVersionManager() {
        this(NamingConstants.Version.VERSION);
    }

    public SimpleDependencyNotationVersionManager(String key) {
        this.key = key;
    }

    @Override
    public String encode(Map<String, String> versionPayload) {
        return versionPayload.computeIfAbsent(key, v -> MappingUtils.getVersionOrMinecraftVersion(versionPayload));
    }

    @Override
    public Map<String, String> decode(String version) {
        final Map<String, String> map = new HashMap<>();
        map.put(NamingConstants.Version.VERSION, version);
        return map;
    }
}
