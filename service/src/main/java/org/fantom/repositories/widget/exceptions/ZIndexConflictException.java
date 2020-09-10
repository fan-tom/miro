package org.fantom.repositories.widget.exceptions;

public class ZIndexConflictException extends Exception {
    public final int zIndex;

    public ZIndexConflictException(int zIndex) {
        this.zIndex = zIndex;
    }
}
