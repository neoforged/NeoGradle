package net.neoforged.gradle.common.tasks;

import net.neoforged.gradle.common.CommonProjectPlugin;
import net.neoforged.gradle.common.caching.CentralCacheService;
import org.gradle.api.DefaultTask;
import org.gradle.api.provider.Property;
import org.gradle.api.services.ServiceReference;
import org.gradle.api.tasks.TaskAction;

public abstract class CleanCache extends DefaultTask {


    @ServiceReference(CommonProjectPlugin.EXECUTE_SERVICE)
    public abstract Property<CentralCacheService> getCacheService();

    public CleanCache() {
        setGroup("neoforged");
        setDescription("Cleans the cache directory");
    }

    @TaskAction
    public void cleanCache() {
        CentralCacheService cacheService = getCacheService().get();
        cacheService.cleanCache(this);
    }
}
