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
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.concurrent.*;

public class DirectoryCache implements ICache {

    private static final int COPY_TIMEOUT_SECONDS = 300; // 5 minutes — large directories like decompiled sources can take a while

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
        if (!file.output().exists()) {
            return;
        }

        final File output = new File(cacheDir, file.output().getName());
        if (!output.exists()) {
            if (!output.mkdirs()) {
                throw new GradleException("Failed to create cache directory: " + output.getAbsolutePath());
            }
        }

        try {
            FileUtils.cleanDirectory(output);
        } catch (IOException e) {
            // If clean fails, delete and recreate the directory
            if (!FileUtils.deleteQuietly(output)) {
                throw new GradleException("Failed to clean cache directory: " + output.getAbsolutePath(), e);
            }
            if (!output.mkdirs()) {
                throw new GradleException("Failed to recreate cache directory: " + output.getAbsolutePath());
            }
        }

        // Use NIO-based copy with timeout instead of FileUtils.copyDirectory which can hang indefinitely
        // on macOS with Gradle test kit temp directories due to native file I/O blocking.
        final Path sourcePath = file.output().toPath();
        try {
            final ExecutorService executor = Executors.newSingleThreadExecutor();
            final Future<?> future = executor.submit(() -> {
                try {
                    Files.walkFileTree(sourcePath, new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                            Path targetDir = output.toPath().resolve(sourcePath.relativize(dir));
                            if (!Files.exists(targetDir)) {
                                Files.createDirectories(targetDir);
                            }
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFile(Path src, BasicFileAttributes attrs) throws IOException {
                            Path target = output.toPath().resolve(sourcePath.relativize(src));
                            Files.copy(src, target, StandardCopyOption.REPLACE_EXISTING);
                            return FileVisitResult.CONTINUE;
                        }
                    });
                } catch (IOException e) {
                    throw new RuntimeException("Failed to walk file tree", e);
                }
            });

            try {
                future.get(COPY_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                future.cancel(true);
                throw new GradleException("Cache copy timed out after " + COPY_TIMEOUT_SECONDS + " seconds. " +
                    "Source: " + file.output().getAbsolutePath() + ", Target: " + output.getAbsolutePath());
            } catch (ExecutionException e) {
                if (e.getCause() instanceof RuntimeException re && re.getCause() instanceof IOException ioEx) {
                    throw ioEx;
                }
                throw new GradleException("Failed to copy directory to cache", e.getCause());
            } finally {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Cache copy interrupted", e);
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
