package net.neoforged.gradle.common.services.caching.cache;

import net.neoforged.gradle.common.services.caching.locking.FileBasedLock;
import net.neoforged.gradle.common.services.caching.locking.LockManager;
import net.neoforged.gradle.common.services.caching.logging.CacheLogger;
import net.neoforged.gradle.common.util.hash.Hashing;
import org.apache.commons.io.FileUtils;
import org.gradle.api.GradleException;

import java.io.File;
import java.io.IOException;

public class DirectoryCache implements ICache {

    private final File cacheDir;

    public DirectoryCache(File cacheDir) {
        this.cacheDir = cacheDir;
    }

    @Override
    public void loadFrom(File file) throws IOException {
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }

        if (file.exists()) {
            final File output = new File(cacheDir, "output");
            FileUtils.cleanDirectory(output);
            FileUtils.copyDirectory(file, output);
        }
    }

    @Override
    public boolean restoreTo(File file) throws IOException {
        final File output = new File(cacheDir, "output");

        if (file.exists()) {
            if (file.isDirectory() && output.exists()) {
                if (Hashing.hashDirectory(file).equals(Hashing.hashDirectory(output))) {
                    return false;
                }
            }

            if (file.isDirectory()) {
                FileUtils.cleanDirectory(file);
            }
            file.delete();
        }

        file.mkdirs();

        if (output.exists()) {
            try {
                FileUtils.copyDirectory(output, file);
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
}
