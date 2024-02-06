package net.neoforged.gradle.common.tasks;

import net.neoforged.gradle.dsl.common.util.CacheFileSelector;
import net.neoforged.gradle.util.UrlConstants;
import org.gradle.api.tasks.TaskAction;

public abstract class MinecraftLauncherFileCacheProvider extends FileCacheProviding {
    public MinecraftLauncherFileCacheProvider() {
        getSelector().set(CacheFileSelector.launcherMetadata());
        // This can change at any point in time.
        getOutputs().upToDateWhen(element -> false);
    }
    
    @TaskAction
    public void doRun() throws Exception {
        downloadJsonTo(UrlConstants.MOJANG_MANIFEST);
    }
}
