package net.neoforged.gradle.common.services.caching.locking;

public interface FileBasedLock extends AutoCloseable {

    void updateAccessTime();

    boolean hasPreviousFailure();

    void markAsSuccess();
}
