package org.fantom.repository;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ClosableReentrantReadWriteLock {

    private final ReentrantReadWriteLock rwLock;

    public ClosableReentrantReadWriteLock(boolean fair) {
        rwLock = new ReentrantReadWriteLock(fair);
    }

    public ClosableReentrantReadWriteLock() {
        rwLock = new ReentrantReadWriteLock();
    }

    public ClosableLock<ReentrantReadWriteLock.ReadLock> readLock() {
        var lock = rwLock.readLock();
        lock.lock();
        return new ClosableLock<>(lock);
    }

    public ClosableLock<ReentrantReadWriteLock.WriteLock> writeLock() {
        var lock = rwLock.writeLock();
        lock.lock();
        return new ClosableLock<>(lock);
    }

    public static class ClosableLock<T extends Lock> implements AutoCloseable {
        private final T lock;

        protected ClosableLock(T lock) {
            this.lock = lock;
        }

        @Override
        public void close() {
            lock.unlock();
        }
    }
}
