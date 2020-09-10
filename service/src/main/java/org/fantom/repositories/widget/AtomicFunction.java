package org.fantom.repositories.widget;

@FunctionalInterface
public interface AtomicFunction<T, R, E extends Exception> {
    R run(T t) throws E;
}
