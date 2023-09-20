package net.neoforged.gradle.common.tasks;

import net.neoforged.gradle.dsl.common.util.CacheFileSelector;
import net.neoforged.gradle.util.UrlConstants;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

@DisableCachingByDefault(because = "This can change at any point in time.")
public abstract class MinecraftLauncherFileCacheProvider extends FileCacheProviding {
    
    public MinecraftLauncherFileCacheProvider() {
        getSelector().set(CacheFileSelector.launcherMetadata());
    }
    
    @TaskAction
    public void doRun() throws Exception {
        downloadJsonTo(UrlConstants.MOJANG_MANIFEST);
    }
}
