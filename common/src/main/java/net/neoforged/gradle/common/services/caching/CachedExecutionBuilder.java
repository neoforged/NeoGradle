package net.neoforged.gradle.common.services.caching;

import net.neoforged.gradle.common.services.caching.cache.DirectoryCache;
import net.neoforged.gradle.common.services.caching.cache.FileCache;
import net.neoforged.gradle.common.services.caching.cache.ICache;
import net.neoforged.gradle.common.services.caching.hasher.TaskHasher;
import net.neoforged.gradle.common.services.caching.jobs.ICacheableJob;
import net.neoforged.gradle.common.services.caching.locking.FileBasedLock;
import net.neoforged.gradle.common.services.caching.logging.CacheLogger;
import net.neoforged.gradle.common.util.hash.HashCode;
import net.neoforged.gradle.common.util.hash.Hasher;
import net.neoforged.gradle.common.util.hash.Hashing;
import net.neoforged.gradle.util.GradleInternalUtils;
import org.apache.commons.io.FileUtils;
import org.gradle.api.GradleException;
import org.gradle.api.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class CachedExecutionBuilder<T> {

    public record LoggingOptions(boolean cacheHits, boolean debug) {}

    public record Options(boolean enabled, File cache, LoggingOptions logging) {}

    private record JobHasher(HashCode taskHash, ICacheableJob<?,?> job, Hasher hasher) {

        public JobHasher(HashCode taskHash, ICacheableJob<?, ?> job) {
            this(taskHash, job, Hashing.sha256().newHasher());
        }

        public HashCode hash() {
            hasher.putHash(taskHash);
            hasher.putString(job.name());
            return hasher.hash();
        }
    }

    private record CacheStatus(@Nullable FileBasedLock lock, boolean shouldExecute, @Nullable ICache cache) implements AutoCloseable {

        private static CacheStatus alwaysUnlocked() {
            return new CacheStatus(null, true, null);
        }

        public static CacheStatus runWithLock(FileBasedLock lock, ICache cache) {
            return new CacheStatus(lock, true, cache);
        }

        public static CacheStatus cachedWithLock(FileBasedLock lock) {
            return new CacheStatus(lock, false, null);
        }

        @NotNull
        public ICache cache() {
            if (cache == null) {
                throw new IllegalStateException("No cache is available.");
            }

            return cache;
        }

        @Override
        public void close() throws Exception {
            if (lock != null) {
                lock.close();
            }
        }

        public void onSuccess() {
            if (lock != null) {
                lock.updateAccessTime();
                lock.markAsSuccess();
            }
        }
    }

    private final Options options;
    private final Task targetTask;
    private final List<ICacheableJob<?, ?>> stages;

    private final CacheLogger logger;

    public CachedExecutionBuilder(Options options, Task targetTask, ICacheableJob<Void, T> initialJob) {
        this(options, targetTask, List.of(initialJob));
    }

    private CachedExecutionBuilder(Options options, Task targetTask, List<ICacheableJob<?, ?>> stages) {
        this.options = options;
        this.targetTask = targetTask;
        this.stages = stages;
        this.logger = new CacheLogger(targetTask, options.logging().cacheHits(), options.logging().debug());
    }

    public <Y> CachedExecutionBuilder<Y> withStage(ICacheableJob<T, Y> job) {
        List<ICacheableJob<?, ?>> newStages = new ArrayList<>(stages);
        newStages.add(job);
        return new CachedExecutionBuilder<>(options, targetTask, newStages);
    }

    /**
     * Executes the cached execution.
     *
     * @throws IOException If an error occurs while executing the cached execution.
     */
    public void execute() throws IOException {
        //When caching is disabled, we do not need to do anything.
        if (!options.enabled()) {
            logger.debug("Caching is disabled, executing all stages.");
            executeAll(
                    (stage) -> CacheStatus.alwaysUnlocked(),
                    (stage, status) -> logger.onCacheMiss(stage)
            );
            return;
        }

        //Create the hash of the task
        final TaskHasher hasher = new TaskHasher(targetTask, logger);
        final HashCode taskHash = hasher.create();

        logger.debug("Task hash: %s".formatted(taskHash));
        executeAll(
                shouldExecuteCachedFor(targetTask, taskHash),
                afterExecution()
        );
    }

    /**
     * Creates a function that determines if a stage should be executed.
     *
     * @param targetTask The target task.
     * @param taskHash The hash of the task.
     * @return The function that determines if a stage should be executed.
     */
    private Function<ICacheableJob<?,?>, CacheStatus> shouldExecuteCachedFor(Task targetTask, HashCode taskHash) {
        return (stage) -> {
            //Create the cache
            final ICache cache = createCache(taskHash, stage);

            //Create and acquire the lock on the cache
            final FileBasedLock lock = cache.createLock(logger);

            try {
                //A cached execution is only healthy if the healthy file exists
                if (lock.hasPreviousFailure()) {
                    logger.debug("Previous failure detected for stage: %s".formatted(stage));
                    return CacheStatus.runWithLock(lock, cache);
                }

                //We have a healthy lock, and the previous execution was successful
                //We can now attempt to restore the cache
                if (!cache.restoreTo(stage.output())) {
                    //No cache restore was needed, we can skip the stage
                    logger.onCacheEquals(stage);
                    GradleInternalUtils.setTaskUpToDate(targetTask, "NeoGradle Cache: Output already exists");
                } else {
                    GradleInternalUtils.setTaskFromCache(targetTask, "NeoGradle Cache: Restored from cache");
                }


                //The cache was restored successfully, we do not need to execute the stage
                return CacheStatus.cachedWithLock(lock);
            } catch (Exception e) {
                throw new GradleException("Failed to restore cache for stage: %s".formatted(stage), e);
            }
        };
    }

    private AfterExecute afterExecution() {
        //Return a consumer that logs the cache hit or miss
        return (stage, status) -> {
            if (status.shouldExecute()) {
                logger.onCacheMiss(stage);
                status.cache().loadFrom(stage.output());
            } else {
                logger.onCacheHit(stage);
            }
        };
    }

    /**
     * Creates a cache for the given task hash and job.
     *
     * @param taskHash The hash of the task.
     * @param job The job to create the cache for.
     * @return The cache for the given task hash and job.
     */
    private ICache createCache(final HashCode taskHash, final ICacheableJob<?,?> job) {
        final JobHasher jobHasher = new JobHasher(taskHash, job);
        final File cacheDir = new File(options.cache(), jobHasher.hash().toString());

        return job.createsDirectory() ? new DirectoryCache(cacheDir) : new FileCache(cacheDir);
    }

    /**
     * Executes the given job with the given input.
     *
     * @param job The job to execute.
     * @param input The input for the job.
     * @return The output of the job.
     * @throws IOException If an error occurs while executing the job.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private Object executeStage(ICacheableJob job, Object input) throws Throwable {
        final File intendedOutput = job.output();

        prepareWorkspace(intendedOutput, job.createsDirectory());

        return job.execute(input);
    }

    /**
     * Prepares the workspace for the given output.
     *
     * @param output The output to prepare the workspace for.
     * @param isDirectory Whether the output is a directory.
     * @throws IOException If an error occurs while preparing the workspace.
     */
    private void prepareWorkspace(final File output, final boolean isDirectory) throws IOException {
        if (isDirectory) {
            if (!output.exists() && !output.mkdirs()) {
                throw new RuntimeException("Failed to create directory: %s".formatted(output.getAbsolutePath()));
            }

            if (output.exists()) {
                FileUtils.cleanDirectory(output);
            }
        } else {
            if (output.exists() && !output.delete()) {
                throw new RuntimeException("Failed to delete file: %s".formatted(output.getAbsolutePath()));
            }
        }
    }

    /**
     * Executes all stages.
     *
     * @param beforeExecute The function to execute before executing a stage.
     * @param afterExecute The consumer to execute after executing a stage.
     */
    private void executeAll(
            final Function<ICacheableJob<?,?>, CacheStatus> beforeExecute,
            final AfterExecute afterExecute
            ) {
        //Holds the current state.
        Object state = null;

        //Loop over all stages and execute them if needed.
        for (ICacheableJob<?, ?> stage : stages) {

            //Grab a cache status for the stage
            try(CacheStatus status = beforeExecute.apply(stage)) {

                //If we should execute the stage, execute it.
                if (status.shouldExecute()) {
                    state = executeStage(stage, state);
                }

                //Run the after execute consumer
                afterExecute.accept(stage, status);

                //Mark the status as successful
                status.onSuccess();
            } catch (Throwable e) {
                //If an exception occurs, throw a Gradle exception.
                //We do not need to notify the status of the failure, as it will be closed and assumes failure
                //if it is not marked as successful.
                throw new GradleException("Failed to execute stage: %s".formatted(stage), e);
            }
        }
    }

    private interface AfterExecute {
        void accept(ICacheableJob<?,?> stage, CacheStatus status) throws Exception;
    }
}
