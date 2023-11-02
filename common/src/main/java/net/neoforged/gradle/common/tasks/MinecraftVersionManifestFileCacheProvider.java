package net.neoforged.gradle.common.tasks;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.neoforged.gradle.dsl.common.util.CacheFileSelector;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
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
        final Gson gson = new Gson();
        
        try(final Reader reader = new FileReader(getLauncherManifest().get().getAsFile())) {
            JsonObject json = gson.fromJson(reader, JsonObject.class);
            
            for (JsonElement e : json.getAsJsonArray("versions")) {
                String v = e.getAsJsonObject().get("id").getAsString();
                if (Objects.equals(minecraftVersion, "+") || v.equals(minecraftVersion)) {
                    downloadJsonTo(e.getAsJsonObject().get("url").getAsString());
                    return;
                }
            }
            
        } catch (IOException e) {
            throw new RuntimeException("Could not read the launcher manifest", e);
        }
        
        throw new IllegalStateException("Could not find the correct version json for version: " + minecraftVersion);
    }
}
