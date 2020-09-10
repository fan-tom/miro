package org.fantom.repositories.widget;

@FunctionalInterface
public interface AtomicAction<T, E extends Exception> {
    void run(T t) throws E;
}
