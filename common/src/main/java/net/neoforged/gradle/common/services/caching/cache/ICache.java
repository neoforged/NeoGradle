package net.neoforged.gradle.common.services.caching.cache;

import net.neoforged.gradle.common.services.caching.jobs.ICacheableJob;
import net.neoforged.gradle.common.services.caching.locking.FileBasedLock;
import net.neoforged.gradle.common.services.caching.logging.CacheLogger;

import java.io.IOException;
import java.util.List;

/**
 * Represents a cache that can be loaded from and restored to a file or directory.
 */
public interface ICache {

    /**
     * Clears the current cache and loads the cache from the given file or directory.
     *
     * @param file The file to load the cache from.
     */
    void loadFrom(List<ICacheableJob.OutputEntry> file) throws IOException;

    /**
     * Restores the cache to the given file or directory.
     *
     * @param file The file to restore the cache to.
     * @return True if the cache was restored, false if the cache was not restored and considered equal.
     */
    boolean restoreTo(List<ICacheableJob.OutputEntry> file) throws IOException;

    /**
     * Creates a lock for the cache.
     *
     * @return The lock for the cache.
     */
    FileBasedLock createLock(CacheLogger logger);

    /**
     * Indicates whether the restore is even possible.
     *
     * @return True when all data is available in the cache and the restore is possible, false otherwise.
     */
    boolean canRestore(final List<ICacheableJob.OutputEntry> output);
}
