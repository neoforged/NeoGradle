package net.neoforged.gradle.common.services.caching.locking;

import java.io.File;

public abstract class HealthFileUsingFileBasedLock implements FileBasedLock {

    private final File healthyFile;

    private boolean hasFailed = true;

    public HealthFileUsingFileBasedLock(File healthyFile) {
        this.healthyFile = healthyFile;
    }

    @Override
    public boolean hasPreviousFailure() {
        return !healthyFile.exists();
    }

    @Override
    public void markAsSuccess() {
        this.hasFailed = false;
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
