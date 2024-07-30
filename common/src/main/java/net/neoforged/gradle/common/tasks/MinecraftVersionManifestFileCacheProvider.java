package net.neoforged.gradle.common.tasks;

import net.neoforged.gradle.dsl.common.util.CacheFileSelector;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;

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

    @Input
    public abstract Property<String> getDownloadUrl();

    private void downloadVersionManifestToCache() {
        downloadJsonTo(getDownloadUrl().get());
    }
}
