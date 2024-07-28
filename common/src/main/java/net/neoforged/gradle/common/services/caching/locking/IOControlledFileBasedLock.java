package net.neoforged.gradle.common.services.caching.locking;

import net.neoforged.gradle.common.services.caching.logging.CacheLogger;

import java.io.File;

public final class IOControlledFileBasedLock extends HealthFileUsingFileBasedLock {

    public static final String HEALTHY_FILE_NAME = "healthy";

    private final File lockFile;
    private final CacheLogger logger;

    private final PIDBasedFileLock pidBasedFileLock;

    public IOControlledFileBasedLock(File lockFile, CacheLogger logger) {
        super(new File(lockFile.getParentFile(), HEALTHY_FILE_NAME));
        this.lockFile = lockFile;
        this.logger = logger;
        
        this.pidBasedFileLock = new PIDBasedFileLock(lockFile, logger);
    }

    @Override
    public void updateAccessTime() {
        if (!lockFile.setLastModified(System.currentTimeMillis())) {
            throw new RuntimeException("Failed to update access time for lock file: %s".formatted(lockFile.getAbsolutePath()));
        }

        logger.debug("Updated access time for lock file: %s".formatted(lockFile.getAbsolutePath()));
    }

    @Override
    public void close() throws Exception {
        //Close the super first, this ensures that the healthy file is created only if the lock was successful
        super.close();
        this.pidBasedFileLock.close();
        logger.debug("Lock file closed: %s".formatted(lockFile.getAbsolutePath()));
    }
}
