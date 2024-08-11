package net.neoforged.gradle.common.services.caching.cache;

import net.neoforged.gradle.common.services.caching.locking.FileBasedLock;
import net.neoforged.gradle.common.services.caching.locking.LockManager;
import net.neoforged.gradle.common.services.caching.logging.CacheLogger;
import net.neoforged.gradle.common.util.hash.Hashing;
import org.apache.commons.io.FileUtils;
import org.gradle.api.GradleException;

import java.io.File;
import java.io.IOException;

public class FileCache implements ICache {

    private final File cacheDir;

    public FileCache(File cacheDir) {
        this.cacheDir = cacheDir;
    }

    @Override
    public void loadFrom(File file) throws IOException {
        final File cacheFile = new File(cacheDir, "output");
        if (cacheFile.exists()) {
            cacheFile.delete();
        }

        // If the file does not exist, there is nothing to load
        if (!file.exists()) {
            return;
        }

        FileUtils.copyFile(file, cacheFile);
    }

    @Override
    public boolean restoreTo(File file) throws IOException {
        final File cacheFile = new File(cacheDir, "output");

        if (file.exists()) {
            if (file.isFile() && cacheFile.exists()) {
                if (Hashing.hashFile(file).equals(Hashing.hashFile(cacheFile))) {
                    return false;
                }
            }

            if (file.isDirectory()) {
                FileUtils.cleanDirectory(file);
            }

            file.delete();
        }

        //If the file exists we can restore it, that means if previous executions did not create an output
        //Then we should not restore it as our cache file would not exist.
        if (cacheFile.exists()) {
            try {
                FileUtils.copyFile(cacheFile, file);
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
}
