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
    private final boolean merge;

    public DirectoryCache(File cacheDir, final boolean merge) {
        this.cacheDir = cacheDir;
        this.merge = merge;
    }

    @Override
    public void loadFrom(File file) throws IOException {
        if (file.exists()) {
            final File output = new File(cacheDir, "output");
            if (!output.exists()) {
                output.mkdirs();
            }

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

            //When we merge we use FileUtils.copyDirectory to merge the results and overwrite anything we don't need.
            if (file.isDirectory() && !merge) {
                FileUtils.cleanDirectory(file);
            }
            //When merge is enabled we don't delete the directory, but we do delete it if it is a file.
            if (file.isFile() || !merge) {
                file.delete();
            }
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
