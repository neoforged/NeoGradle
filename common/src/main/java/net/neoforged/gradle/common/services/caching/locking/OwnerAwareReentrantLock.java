package net.neoforged.gradle.common.services.caching.locking;

import java.util.concurrent.locks.ReentrantLock;

public final class OwnerAwareReentrantLock extends ReentrantLock {
    @Override
    public Thread getOwner() {
        return super.getOwner();
    }
}
