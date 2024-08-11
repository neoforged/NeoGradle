package net.neoforged.gradle.common.services.caching.locking;

import com.google.common.collect.Lists;
import net.neoforged.gradle.common.services.caching.logging.CacheLogger;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class PIDBasedFileLock implements AutoCloseable {

    private static final Map<String, OwnerAwareReentrantLock> FILE_LOCKS = new ConcurrentHashMap<>();
    private final File lockFile;
    private final CacheLogger logger;

    public PIDBasedFileLock(File lockFile, CacheLogger logger) {
        this.lockFile = lockFile;
        this.logger = logger;

        this.lockFile();
    }

    private void lockFile() {
        logger.debug("Attempting to acquire lock on file: " + lockFile.getAbsolutePath());
        while (!attemptFileLock()) {
            //We attempt a lock every 500ms
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException("Failed to acquire lock on file: " + lockFile.getAbsolutePath(), e);
            }
        }
        logger.debug("Lock acquired on file: " + lockFile.getAbsolutePath());
    }

    private synchronized boolean attemptFileLock() {
        try {
            if (!lockFile.exists()) {
                //No lock file exists, create one
                lockFile.getParentFile().mkdirs();
                Files.write(lockFile.toPath(), String.valueOf(ProcessHandle.current().pid()).getBytes(), StandardOpenOption.CREATE_NEW);
                lockFileForCurrentProcess();
                return true;
            }

            //Lock file exists, check if we are the owner
            for (String s : Files.readAllLines(lockFile.toPath())) {
                //Get the written pid.
                int pid = Integer.parseInt(s);
                if (ProcessHandle.current().pid() == pid) {
                    logger.debug("Lock file is owned by current process: " + lockFile.getAbsolutePath() + " pid: " + pid);
                    lockFileForCurrentProcess();
                    return true;
                }

                //Check if the process is still running
                if (ProcessHandle.of(pid).isEmpty()) {
                    //Process is not running, we can take over the lock
                    logger.debug("Lock file is owned by a killed process: " + lockFile.getAbsolutePath() + " taking over. Old pid: " + pid);
                    Files.write(lockFile.toPath(), String.valueOf(ProcessHandle.current().pid()).getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
                    lockFileForCurrentProcess();
                    return true;
                }


                logger.debug("Lock file is owned by another process: " + lockFile.getAbsolutePath() + " pid: " + pid);
                //Process is still running, we can't take over the lock
                return false;
            }

            //No pid found in lock file, we can take over the lock
            logger.debug("Lock file is empty: " + lockFile.getAbsolutePath());
            Files.write(lockFile.toPath(), String.valueOf(ProcessHandle.current().pid()).getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
            lockFileForCurrentProcess();
            return true;
        } catch (Exception e) {
            logger.debug("Failed to acquire lock on file: " + lockFile.getAbsolutePath() + " -  Failure message: " + e.getLocalizedMessage(), e);
            return false;
        }
    }

    private void lockFileForCurrentProcess() {
        final OwnerAwareReentrantLock lock = FILE_LOCKS.computeIfAbsent(lockFile.getAbsolutePath(), s1 -> new OwnerAwareReentrantLock());
        if (lock.getOwner() != null) {
            logger.debug("Lock file is held by thread: " + lock.getOwner().getId() + " - " + lock.getOwner().getName() + " current thread: " + Thread.currentThread().getId() + " - " + Thread.currentThread().getName());
        } else {
            logger.debug("Lock file is not held by any thread");
        }
        lock.lock();
    }

    @Override
    public void close() throws Exception {
        logger.debug("Releasing lock on file: " + lockFile.getAbsolutePath());
        Files.write(lockFile.toPath(), Lists.newArrayList(), StandardOpenOption.TRUNCATE_EXISTING);
        if (FILE_LOCKS.containsKey(lockFile.getAbsolutePath())) {
            logger.debug("Unlocking: " + Thread.currentThread().getId() + " - " + Thread.currentThread().getName());
            final OwnerAwareReentrantLock lock = FILE_LOCKS.get(lockFile.getAbsolutePath());
            lock.unlock();
            if (lock.getHoldCount() == 0) {
                FILE_LOCKS.remove(lockFile.getAbsolutePath());
            }
        }
    }
}
