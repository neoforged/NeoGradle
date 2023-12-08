package net.neoforged.gradle.common.tasks;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.neoforged.gradle.common.util.SerializationUtils;
import net.neoforged.gradle.dsl.common.util.CacheFileSelector;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;

import java.util.Objects;

@CacheableTask
public abstract class MinecraftVersionManifestFileCacheProvider extends FileCacheProviding {

    public MinecraftVersionManifestFileCacheProvider() {
        getSelector().set(getMinecraftVersion().map(CacheFileSelector::forVersionJson));
        getMinecraftVersion().convention("+");
    }
    
    @TaskAction
    public void doDownload() {
        downloadVersionManifestToCache();
    }

    @Input
    @Optional
    public abstract Property<String> getMinecraftVersion();
    
    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getLauncherManifest();
    
    private void downloadVersionManifestToCache() {
        final String minecraftVersion = getMinecraftVersion().get();

        JsonObject json = SerializationUtils.fromJson(getLauncherManifest().get().getAsFile(), JsonObject.class);

        for (JsonElement e : json.getAsJsonArray("versions")) {
            String v = e.getAsJsonObject().get("id").getAsString();
            if (Objects.equals(minecraftVersion, "+") || v.equals(minecraftVersion)) {
                downloadJsonTo(e.getAsJsonObject().get("url").getAsString());
                return;
            }
        }

        throw new IllegalStateException("Could not find the correct version json for version: " + minecraftVersion);
    }
}
