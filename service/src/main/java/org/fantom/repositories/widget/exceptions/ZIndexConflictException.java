package org.fantom.repositories.widget.exceptions;

public class ZIndexConflictException extends Exception {
    public final Integer zIndex;

    public String zIndexAsString() {
        if (zIndex == null) {
            return "[unknown]";
        } else {
            return zIndex.toString();
        }
    }

    public ZIndexConflictException() {
        zIndex = null;
    }

    public ZIndexConflictException(Integer zIndex) {
        this.zIndex = zIndex;
    }
}
