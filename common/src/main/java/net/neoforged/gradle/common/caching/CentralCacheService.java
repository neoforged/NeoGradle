package net.neoforged.gradle.common.caching;

import com.google.common.collect.Lists;
import net.neoforged.gradle.common.util.hash.HashCode;
import net.neoforged.gradle.common.util.hash.HashFunction;
import net.neoforged.gradle.common.util.hash.Hasher;
import net.neoforged.gradle.common.util.hash.Hashing;
import net.neoforged.gradle.util.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.services.BuildService;
import org.gradle.api.services.BuildServiceParameters;
import org.gradle.api.tasks.TaskInputs;
import org.gradle.internal.impldep.org.joda.time.DateTime;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.DateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This is a cache service, which holds no directory information, yet.
 * I tried different ways of adding a directory property, but it always failed during isolation of the params.
 * For now please make sure that consuming tasks have a directory property, which is set to their cache directory.
 */
public abstract class CentralCacheService implements BuildService<CentralCacheService.Parameters> {

    public static final String CACHING_PROPERTY_PREFIX = "net.neoforged.gradle.caching.";
    public static final String CACHE_DIRECTORY_PROPERTY = CACHING_PROPERTY_PREFIX + "cacheDirectory";
    public static final String LOG_CACHE_HITS_PROPERTY = CACHING_PROPERTY_PREFIX + "logCacheHits";
    public static final String MAX_CACHE_SIZE_PROPERTY = CACHING_PROPERTY_PREFIX + "maxCacheSize";
    public static final String DEBUG_CACHE_PROPERTY = CACHING_PROPERTY_PREFIX + "debug";
    public static final String IS_ENABLED_PROPERTY = CACHING_PROPERTY_PREFIX + "enabled";
    public static final String HEALTHY_FILE_NAME = "healthy";

    public static void register(Project project, String name, boolean isolated) {
        project.getGradle().getSharedServices().registerIfAbsent(
                name,
                CentralCacheService.class,
                spec -> {
                    if (isolated) {
                        spec.getMaxParallelUsages().set(1);
                    }

                    spec.getParameters().getCacheDirectory()
                            .fileProvider(project.getProviders().gradleProperty(CACHE_DIRECTORY_PROPERTY)
                                    .map(File::new)
                                    .orElse(new File(new File(project.getGradle().getGradleUserHomeDir(), "caches"), name)));
                    spec.getParameters().getLogCacheHits().set(project.getProviders().gradleProperty(LOG_CACHE_HITS_PROPERTY).map(Boolean::parseBoolean).orElse(false));
                    spec.getParameters().getMaxCacheSize().set(project.getProviders().gradleProperty(MAX_CACHE_SIZE_PROPERTY).map(Integer::parseInt).orElse(100));
                    spec.getParameters().getDebugCache().set(project.getProviders().gradleProperty(DEBUG_CACHE_PROPERTY).map(Boolean::parseBoolean).orElse(false));
                    spec.getParameters().getIsEnabled().set(project.getProviders().gradleProperty(IS_ENABLED_PROPERTY).map(Boolean::parseBoolean).orElse(true));
                }
        );
    }

    private final LockManager lockManager = new LockManager();

    public void cleanCache(Task cleaningTask) {
        if (!getParameters().getIsEnabled().get()) {
            debugLog(cleaningTask, "Cache is disabled, skipping clean");
            return;
        }

        final File cacheDirectory = getParameters().getCacheDirectory().get().getAsFile();

        if (cacheDirectory.exists()) {
            try (Stream<Path> stream = Files.walk(cacheDirectory.toPath())) {
                final Set<Path> lockFilesToDelete = stream.filter(path -> path.getFileName().toString().equals("lock"))
                        .sorted(Comparator.<Path>comparingLong(p -> p.toFile().lastModified()).reversed())
                        .skip(getParameters().getMaxCacheSize().get())
                        .collect(Collectors.toSet());

                for (Path path : lockFilesToDelete) {
                    final File cacheFileDirectory = path.getParent().toFile();
                    debugLog(cleaningTask, "Deleting cache directory for clean: " + cacheFileDirectory.getAbsolutePath());
                    FileUtils.delete(cacheFileDirectory.toPath());
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void doCached(Task task, DoCreate onCreate, Provider<RegularFile> target) throws Throwable {
        if (!getParameters().getIsEnabled().get()) {
            debugLog(task, "Cache is disabled, skipping cache");
            onCreate.create();
            return;
        }

        final TaskHasher hasher = new TaskHasher(task);
        final String hashDirectoryName = hasher.create().toString();
        final Directory cacheDirectory = getParameters().getCacheDirectory().get().dir(hashDirectoryName);
        final RegularFile targetFile = target.get();

        debugLog(task, "Cache directory: " + cacheDirectory.getAsFile().getAbsolutePath());
        debugLog(task, "Target file: " + targetFile.getAsFile().getAbsolutePath());

        final File lockFile = new File(cacheDirectory.getAsFile(), "lock");
        debugLog(task, "Lock file: " + lockFile.getAbsolutePath());
        executeCacheLookupOrCreation(task, onCreate, lockFile, cacheDirectory, targetFile);
        debugLog(task, "Cached task: " + task.getPath() + " finished");
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void executeCacheLookupOrCreation(Task task, DoCreate onCreate, File lockFile, Directory cacheDirectory, RegularFile targetFile) throws Throwable {
        if (!lockFile.exists()) {
            debugLog(task, "Lock file does not exist: " + lockFile.getAbsolutePath());
            try {
                lockFile.getParentFile().mkdirs();
                lockFile.createNewFile();
            } catch (IOException e) {
                throw new GradleException("Failed to create lock file: " + lockFile.getAbsolutePath(), e);
            }
        }

        // Acquiring an exclusive lock on the file
        debugLog(task, "Acquiring lock on file: " + lockFile.getAbsolutePath());
        try(FileBasedLock fileBasedLock = lockManager.createLock(task, lockFile)) {
            try {
                fileBasedLock.updateAccessTime();
                debugLog(task, "Lock acquired on file: " + lockFile.getAbsolutePath());

                executeCacheLookupOrCreationLocked(task, onCreate, cacheDirectory, targetFile.getAsFile(), fileBasedLock.hasPreviousFailure());

                // Release the lock when done
                debugLog(task, "Releasing lock on file: " + lockFile.getAbsolutePath());
            } catch (Exception ex) {
                debugLog(task, "Exception occurred while executing cached task: " + targetFile.getAsFile().getAbsolutePath(), ex);
                fileBasedLock.markAsFailed();
                throw new GradleException("Cached execution failed for: " + task.getName(), ex);
            }
        }
    }

    private void executeCacheLookupOrCreationLocked(Task task, DoCreate onCreate, Directory cacheDirectory, File targetFile, boolean failedPreviously) throws Throwable {
        final File cacheFile = new File(cacheDirectory.getAsFile(), targetFile.getName());
        final File noCacheFile = new File(cacheDirectory.getAsFile(), "nocache");

        if (failedPreviously) {
            //Previous execution failed
            debugLog(task, "Last cache run failed: " + cacheFile.getAbsolutePath());
            Files.deleteIfExists(cacheFile.toPath());
            Files.deleteIfExists(noCacheFile.toPath());
        }

        if (noCacheFile.exists()) {
            //Previous execution indicated no output
            debugLog(task, "Last cache run indicated no output: " + noCacheFile.getAbsolutePath());
            logCacheHit(task, noCacheFile);
            task.setDidWork(false);
            Files.deleteIfExists(targetFile.toPath());
            return;
        }
        if (cacheFile.exists()) {
            debugLog(task, "Cached file exists: " + cacheFile.getAbsolutePath());
            if (targetFile.exists() && Hashing.hashFile(cacheFile).equals(Hashing.hashFile(targetFile))) {
                debugLog(task, "Cached file equals target file");
                task.setDidWork(false);
                logCacheEquals(task, cacheFile);
                return;
            }

            debugLog(task, "Cached file does not equal target file");
            logCacheHit(task, cacheFile);
            Files.deleteIfExists(targetFile.toPath());
            Files.copy(cacheFile.toPath(), targetFile.toPath());
            task.setDidWork(false);
        } else {
            debugLog(task, "Cached file does not exist: " + cacheFile.getAbsolutePath());
            logCacheMiss(task);

            debugLog(task, "Creating output: " + targetFile.getAbsolutePath());
            File createdFile = onCreate.create();
            debugLog(task, "Created output: " + createdFile.getAbsolutePath());

            if (!createdFile.exists()) {
                //No output was created
                debugLog(task, "No output was created: " + createdFile.getAbsolutePath());
                Files.createFile(noCacheFile.toPath());
                Files.deleteIfExists(cacheFile.toPath());
            } else {
                //Output was created
                debugLog(task, "Output was created: " + createdFile.getAbsolutePath());
                Files.deleteIfExists(noCacheFile.toPath());
                Files.copy(createdFile.toPath(), cacheFile.toPath());
            }
            task.setDidWork(true);
        }
    }

    private void logCacheEquals(Task task, File cacheFile) {
        if (getParameters().getLogCacheHits().get()) {
            task.getLogger().lifecycle("Cache equal for task {} from {}", task.getPath(), cacheFile.getAbsolutePath());
        }
    }

    private void logCacheHit(Task task, File cacheFile) {
        if (getParameters().getLogCacheHits().get()) {
            task.getLogger().lifecycle("Cache hit for task {} from {}", task.getPath(), cacheFile.getAbsolutePath());
        }
    }

    private void logCacheMiss(Task task) {
        if (getParameters().getLogCacheHits().get()) {
            task.getLogger().lifecycle("Cache miss for task {}", task.getPath());
        }
    }

    private void debugLog(Task task, String message) {
        if (getParameters().getDebugCache().get()) {
            task.getLogger().lifecycle( " > [" + new Date(System.currentTimeMillis()) + "] (" + ProcessHandle.current().pid() + "): " + message);
        }
    }

    private void debugLog(Task task, String message, Exception e) {
        if (getParameters().getDebugCache().get()) {
            task.getLogger().lifecycle( " > [" + new Date(System.currentTimeMillis()) + "] (" + ProcessHandle.current().pid() + "): " + message, e);
        }
    }

    public interface DoCreate {
        File create() throws Throwable;
    }

    public interface Parameters extends BuildServiceParameters {

        DirectoryProperty getCacheDirectory();

        Property<Boolean> getLogCacheHits();

        Property<Integer> getMaxCacheSize();

        Property<Boolean> getDebugCache();

        Property<Boolean> getIsEnabled();
    }

    private final class TaskHasher {
        private final HashFunction hashFunction = Hashing.sha256();
        private final Hasher hasher = hashFunction.newHasher();

        private final Task task;

        public TaskHasher(Task task) {
            this.task = task;
        }

        public void hash() throws IOException {
            debugLog(task, "Hashing task: " + task.getPath());
            hasher.putString(task.getClass().getName());

            final TaskInputs taskInputs = task.getInputs();
            hash(taskInputs);
        }

        public void hash(TaskInputs inputs) throws IOException {
            debugLog(task, "Hashing task inputs: " + task.getPath());
            inputs.getProperties().forEach((key, value) -> {
                debugLog(task, "Hashing task input property: " + key);
                hasher.putString(key);
                debugLog(task, "Hashing task input property value: " + value);
                hasher.put(value, false); //We skin unknown types (mostly file collections)
            });

            for (File file : inputs.getFiles()) {
                debugLog(task, "Hashing task input file: " + file.getAbsolutePath());
                hasher.putString(file.getName());
                final HashCode code = hashFunction.hashFile(file);
                debugLog(task, "Hashing task input file hash: " + code);
                hasher.putHash(code);
            }
        }

        public HashCode create() throws IOException {
            hash();
            return hasher.hash();
        }
    }

    private interface FileBasedLock extends AutoCloseable {

        void updateAccessTime();

        boolean hasPreviousFailure();

        void markAsFailed();
    }

    private final class LockManager {
        public FileBasedLock createLock(Task task, File lockFile) {
            if (WindowsFileBasedLock.isSupported()) {
                return new WindowsFileBasedLock(task, lockFile);
            } else {
                return new IOControlledFileBasedLock(task, lockFile);
            }
        }
    }

    private static abstract class HealthFileUsingFileBasedLock implements FileBasedLock {

        private final File healthyFile;

        private boolean hasFailed = false;

        private HealthFileUsingFileBasedLock(File healthyFile) {
            this.healthyFile = healthyFile;

            if (this.healthyFile.exists() && !this.healthyFile.delete())
                throw new IllegalStateException("Failed to delete healthy marker file: " + this.healthyFile.getAbsolutePath());
        }

        @Override
        public boolean hasPreviousFailure() {
            return hasFailed || !healthyFile.exists();
        }

        @Override
        public void markAsFailed() {
            this.hasFailed = true;
        }

        @Override
        public void close() throws Exception {
            //Creation of the healthy file is the last thing we do, so if it exists, we know the lock was successful
            if (!hasFailed) {
                this.healthyFile.createNewFile();
            } else if (this.healthyFile.exists() && !this.healthyFile.delete()) {
                //We grabbed a lock together with another process, the other process succeeded, we failed, but also failed to delete our healthy marker.
                //Something is wrong, we should not continue.
                throw new IllegalStateException("Failed to delete healthy marker file: " + this.healthyFile.getAbsolutePath());
            }
        }
    }

    private final class WindowsFileBasedLock extends HealthFileUsingFileBasedLock {

        private static boolean isSupported() {
            return SystemUtils.IS_OS_WINDOWS;
        }

        private final Task task;
        private final File lockFile;
        private final NioBasedFileLock nioBasedFileLock;

        private WindowsFileBasedLock(Task task, File lockFile) {
            super(new File(lockFile.getParentFile(), HEALTHY_FILE_NAME));

            if (!isSupported() || !NioBasedFileLock.isSupported()) {
                throw new UnsupportedOperationException("Windows file locks are not supported on this platform, or NIO based locking is not supported!");
            }

            this.task = task;
            this.lockFile = lockFile;
            this.nioBasedFileLock = new NioBasedFileLock(task, lockFile);
        }

        @Override
        public void updateAccessTime() {
            if (!lockFile.setLastModified(System.currentTimeMillis())) {
                throw new RuntimeException("Failed to update access time for lock file: " + lockFile.getAbsolutePath());
            }

            debugLog(task, "Updated access time for lock file: " + lockFile.getAbsolutePath());
        }

        @Override
        public void close() throws Exception {
            //Close the super first, this ensures that the healthy file is created only if the lock was successful
            super.close();
            this.nioBasedFileLock.close();
            debugLog(task, "Lock file closed: " + lockFile.getAbsolutePath());
        }
    }

    private final class NioBasedFileLock implements AutoCloseable {

        public static boolean isSupported() {
            return SystemUtils.IS_OS_WINDOWS;
        }

        private final Task task;
        private final File lockFile;
        private final RandomAccessFile lockFileAccess;
        private final FileChannel fileChannel;
        private final FileLock fileLock;

        public NioBasedFileLock(Task task, File lockFile) {
            if (!isSupported()) {
                throw new UnsupportedOperationException("NIO file locks are not supported on this platform");
            }

            this.task = task;
            this.lockFile = lockFile;

            try {
                this.lockFileAccess = new RandomAccessFile(lockFile, "rw");
                this.fileChannel = this.lockFileAccess.getChannel();
                this.fileLock = this.fileChannel.lock();

                debugLog(task, "Acquired lock on file: " + lockFile.getAbsolutePath());
            } catch (IOException e) {
                throw new RuntimeException("Failed to acquire lock on file: " + lockFile.getAbsolutePath(), e);
            }
        }

        @Override
        public void close() throws Exception {
            fileLock.release();
            fileChannel.close();
            lockFileAccess.close();

            debugLog(task, "Released lock on file: " + lockFile.getAbsolutePath());
        }
    }

    private final class IOControlledFileBasedLock extends HealthFileUsingFileBasedLock {

        private final Task task;
        private final File lockFile;
        private final PIDBasedFileLock pidBasedFileLock;

        private IOControlledFileBasedLock(Task task, File lockFile) {
            super(new File(lockFile.getParentFile(), "healthy"));
            this.task = task;
            this.lockFile = lockFile;
            this.pidBasedFileLock = new PIDBasedFileLock(task, lockFile);
        }

        @Override
        public void updateAccessTime() {
            if (!lockFile.setLastModified(System.currentTimeMillis())) {
                throw new RuntimeException("Failed to update access time for lock file: " + lockFile.getAbsolutePath());
            }

            debugLog(task, "Updated access time for lock file: " + lockFile.getAbsolutePath());
        }

        @Override
        public void close() throws Exception {
            //Close the super first, this ensures that the healthy file is created only if the lock was successful
            super.close();
            this.pidBasedFileLock.close();
            debugLog(task, "Lock file closed: " + lockFile.getAbsolutePath());
        }
    }

    private final class PIDBasedFileLock implements AutoCloseable {

        private final Task task;
        private final File lockFile;

        private PIDBasedFileLock(Task task, File lockFile) {
            this.task = task;
            this.lockFile = lockFile;
            this.lockFile();
        }

        private void lockFile() {
            while (!attemptFileLock()) {
                //We attempt a lock every 500ms
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    throw new RuntimeException("Failed to acquire lock on file: " + lockFile.getAbsolutePath(), e);
                }
            }
        }

        private boolean attemptFileLock() {
            try {
                if (!lockFile.exists()) {
                    //No lock file exists, create one
                    Files.write(lockFile.toPath(), String.valueOf(ProcessHandle.current().pid()).getBytes(), StandardOpenOption.CREATE_NEW);
                    return true;
                }

                //Lock file exists, check if we are the owner
                for (String s : Files.readAllLines(lockFile.toPath())) {
                    //Get the written pid.
                    int pid = Integer.parseInt(s);
                    if (ProcessHandle.current().pid() == pid) {
                        debugLog(task, "Lock file is owned by current process: " + lockFile.getAbsolutePath() + " pid: " + pid);
                        return true;
                    }

                    //Check if the process is still running
                    if (ProcessHandle.of(pid).isEmpty()) {
                        //Process is not running, we can take over the lock
                        debugLog(task, "Lock file is owned by a killed process: " + lockFile.getAbsolutePath() + " taking over. Old pid: " + pid);
                        Files.write(lockFile.toPath(), String.valueOf(ProcessHandle.current().pid()).getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
                        return true;
                    }


                    debugLog(task, "Lock file is owned by another process: " + lockFile.getAbsolutePath() + " pid: " + pid);
                    //Process is still running, we can't take over the lock
                    return false;
                }

                //No pid found in lock file, we can take over the lock
                debugLog(task, "Lock file is empty: " + lockFile.getAbsolutePath());
                Files.write(lockFile.toPath(), String.valueOf(ProcessHandle.current().pid()).getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
                return true;
            } catch (Exception e) {
                debugLog(task, "Failed to acquire lock on file: " + lockFile.getAbsolutePath() + " -  Failure message: " + e.getLocalizedMessage(), e);
                return false;
            }
        }

        @Override
        public void close() throws Exception {
            debugLog(task, "Releasing lock on file: " + lockFile.getAbsolutePath());
            Files.write(lockFile.toPath(), Lists.newArrayList(), StandardOpenOption.TRUNCATE_EXISTING);
        }
    }
}
