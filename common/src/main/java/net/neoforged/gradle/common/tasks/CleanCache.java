package net.neoforged.gradle.common.tasks;

import net.neoforged.gradle.common.services.caching.CachedExecutionService;
import org.gradle.api.DefaultTask;
import org.gradle.api.provider.Property;
import org.gradle.api.services.ServiceReference;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;

public abstract class CleanCache extends DefaultTask {


    @ServiceReference(CachedExecutionService.NAME)
    public abstract Property<CachedExecutionService> getCacheService();

    public CleanCache() {
        setGroup("neogradle");
        setDescription("Cleans the cache directory");
    }

    @TaskAction
    public void cleanCache() throws IOException {
        CachedExecutionService cacheService = getCacheService().get();
        cacheService.clean();
    }
}
