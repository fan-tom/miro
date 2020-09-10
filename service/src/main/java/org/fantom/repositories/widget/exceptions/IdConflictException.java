package org.fantom.repositories.widget.exceptions;

public class IdConflictException extends Exception {
    // no generic exceptions, shit
    public final Object id;

    IdConflictException(Object id) {
        this.id = id;
    }
}
