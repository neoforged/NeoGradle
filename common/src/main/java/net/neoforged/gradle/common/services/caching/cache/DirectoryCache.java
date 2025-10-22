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

public class DirectoryCache implements ICache {

    private final File cacheDir;
    private final boolean merge;

    public DirectoryCache(File cacheDir, final boolean merge) {
        this.cacheDir = cacheDir;
        this.merge = merge;
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
        if (file.output().exists()) {
            final File output = new File(cacheDir, file.output().getName());
            if (!output.exists()) {
                output.mkdirs();
            }

            FileUtils.cleanDirectory(output);
            FileUtils.copyDirectory(file.output(), output);
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

    public boolean restoreTo(ICacheableJob.OutputEntry file) throws IOException {
        final File output = new File(cacheDir, file.output().getName());

        if (file.output().exists()) {
            if (file.isDirectory() && output.exists()) {
                if (Hashing.hashDirectory(file.output()).equals(Hashing.hashDirectory(output))) {
                    return false;
                }
            }

            //When we merge we use FileUtils.copyDirectory to merge the results and overwrite anything we don't need.
            if (file.isDirectory() && !merge) {
                FileUtils.cleanDirectory(file.output());
            }
            //When merge is enabled we don't delete the directory, but we do delete it if it is a file.
            if (file.output().isFile() || !merge) {
                file.output().delete();
            }
        }

        file.output().mkdirs();

        if (output.exists()) {
            try {
                FileUtils.copyDirectory(output, file.output());
            } catch (IOException e) {
                throw new GradleException("Failed to restore cache.", e);
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
        final File output = new File(cacheDir, file.output().getName());
        return output.exists() && output.isDirectory();
    }
}
