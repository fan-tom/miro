package org.fantom.repositories.widget;

@FunctionalInterface
public interface IdGenerator<ID> {
    ID generate();
}
