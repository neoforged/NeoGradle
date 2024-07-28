package net.neoforged.gradle.common.services.caching.locking;

import net.neoforged.gradle.common.services.caching.logging.CacheLogger;

import java.io.File;

public final class LockManager {

    public static final String LOCK_FILE_NAME = "lock";

    public static FileBasedLock createLock(File target, CacheLogger logger) {
        final File lockFile = new File(target, LOCK_FILE_NAME);

        return new IOControlledFileBasedLock(lockFile, logger);
    }
}
