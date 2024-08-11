package net.neoforged.gradle.common.services.caching.logging;

import net.neoforged.gradle.common.services.caching.jobs.ICacheableJob;
import org.gradle.api.Task;

public class CacheLogger {
    
    private final Task task;
    private final boolean debug;
    private final boolean cacheHits;

    public CacheLogger(Task task, boolean debug, boolean cacheHits) {
        this.task = task;
        this.debug = debug;
        this.cacheHits = cacheHits;
    }
    
    public void onCacheEquals(ICacheableJob<?,?> stage) {
        if (cacheHits) {
            task.getLogger().lifecycle("Cache equal for task {} from {}", task.getPath(), stage.name());
        }
    }

    public void onCacheHit(ICacheableJob<?,?> stage) {
        if (cacheHits) {
            task.getLogger().lifecycle("Cache hit for task {} from {}", task.getPath(), stage.name());
        }
    }

    public void onCacheMiss(ICacheableJob<?,?> stage) {
        if (cacheHits) {
            task.getLogger().lifecycle("Cache miss for task {} from {}", task.getPath(), stage.name());
        }
    }

    public void debug(String message) {
        if (debug) {
            task.getLogger().lifecycle(" > [" + System.currentTimeMillis() + "] (" + ProcessHandle.current().pid() + "): " + message);
        }
    }

    public void debug(String message, Exception e) {
        if (debug) {
            task.getLogger().lifecycle(" > [" + System.currentTimeMillis() + "] (" + ProcessHandle.current().pid() + "): " + message, e);
        }
    }
}

