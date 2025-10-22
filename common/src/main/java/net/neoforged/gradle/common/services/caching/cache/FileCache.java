package net.neoforged.gradle.common.services.caching.cache;

import net.neoforged.gradle.common.services.caching.jobs.ICacheableJob;
import net.neoforged.gradle.common.services.caching.locking.FileBasedLock;
import net.neoforged.gradle.common.services.caching.locking.LockManager;
import net.neoforged.gradle.common.services.caching.logging.CacheLogger;
import net.neoforged.gradle.common.util.hash.Hashing;
import org.apache.commons.io.FileUtils;
import org.gradle.api.GradleException;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class FileCache implements ICache {

    private final File cacheDir;

    public FileCache(File cacheDir) {
        this.cacheDir = cacheDir;
    }

    @Override
    public void loadFrom(final List<ICacheableJob.OutputEntry> file) throws IOException
    {
        for (final ICacheableJob.OutputEntry file1 : file)
        {
            loadFrom(file1);
        }
    }

    public void loadFrom(ICacheableJob.OutputEntry file) throws IOException {
        final File cacheFile = new File(cacheDir, file.output().getName());
        if (cacheFile.exists()) {
            cacheFile.delete();
        }

        // If the file does not exist, there is nothing to load
        if (!file.output().exists()) {
            return;
        }

        FileUtils.copyFile(file.output(), cacheFile);
    }

    @Override
    public boolean restoreTo(final List<ICacheableJob.OutputEntry> file) throws IOException
    {
        boolean restored = false;
        for (final ICacheableJob.OutputEntry file1 : file)
        {
            restored = restoreTo(file1);
        }

        return restored;
    }

    public boolean restoreTo(ICacheableJob.OutputEntry file) throws IOException {
        final File cacheFile = new File(cacheDir, file.output().getName());

        if (file.output().exists()) {
            if (file.output().isFile() && cacheFile.exists()) {
                if (Hashing.hashFile(file.output()).equals(Hashing.hashFile(cacheFile))) {
                    return false;
                }
            }

            if (file.isDirectory()) {
                FileUtils.cleanDirectory(file.output());
            }

            file.output().delete();
        }

        //If the file exists we can restore it, that means if previous executions did not create an outputs
        //Then we should not restore it as our cache file would not exist.
        if (cacheFile.exists()) {
            try {
                FileUtils.copyFile(cacheFile, file.output());
            } catch (IOException e) {
                throw new GradleException("Failed to restore cache. Copying of the cache file failed.", e);
            }
        }

        return true;
    }

    @Override
    public FileBasedLock createLock(CacheLogger logger) {
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

    public boolean canRestore(final ICacheableJob.OutputEntry file)
    {
        final File cacheFile = new File(cacheDir, file.output().getName());
        return cacheFile.exists() && cacheFile.isFile();
    }
}
