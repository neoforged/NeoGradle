package net.neoforged.gradle.common.services.caching.cache;

import net.neoforged.gradle.common.services.caching.jobs.ICacheableJob;
import net.neoforged.gradle.common.services.caching.locking.FileBasedLock;
import net.neoforged.gradle.common.services.caching.locking.LockManager;
import net.neoforged.gradle.common.services.caching.logging.CacheLogger;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class MultiEntryCache implements ICache
{

    private final File cacheDir;

    public MultiEntryCache(final File cacheDir) {
        this.cacheDir = cacheDir;
    }

    @Override
    public void loadFrom(final List<ICacheableJob.OutputEntry> file) throws IOException
    {
        for (final ICacheableJob.OutputEntry outputEntry : file)
        {
            if (outputEntry.isDirectory()) {
                new DirectoryCache(cacheDir, outputEntry.mergesDirectory()).loadFrom(outputEntry);
            } else {
                new FileCache(cacheDir).loadFrom(outputEntry);
            }
        }
    }

    @Override
    public boolean restoreTo(final List<ICacheableJob.OutputEntry> file) throws IOException
    {
        boolean restored = true;
        for (final ICacheableJob.OutputEntry file1 : file)
        {
            if (!restoreTo(file1))
                restored = false;
        }

        return restored;
    }

    public boolean restoreTo(ICacheableJob.OutputEntry outputEntry) throws IOException {
        if (outputEntry.isDirectory()) {
            return new DirectoryCache(cacheDir, outputEntry.mergesDirectory()).restoreTo(outputEntry);
        } else {
            return new FileCache(cacheDir).restoreTo(outputEntry);
        }
    }

    @Override
    public FileBasedLock createLock(final CacheLogger logger)
    {
        return LockManager.createLock(cacheDir, logger);
    }

    @Override
    public boolean canRestore(final List<ICacheableJob.OutputEntry> output)
    {
        boolean restoreable = true;
        for (final ICacheableJob.OutputEntry file : output)
        {
            if (!canRestore(file))
                restoreable = false;
        }
        return restoreable;
    }

    public boolean canRestore(ICacheableJob.OutputEntry outputEntry)
    {
        if (outputEntry.isDirectory()) {
            return new DirectoryCache(cacheDir, outputEntry.mergesDirectory()).canRestore(outputEntry);
        } else {
            return new FileCache(cacheDir).canRestore(outputEntry);
        }
    }
}
