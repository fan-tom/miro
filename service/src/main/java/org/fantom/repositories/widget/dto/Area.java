package org.fantom.repositories.widget.dto;

public class Area {

    /**
     * x-coordinate of left edge
     */
    public final int left;

    /**
     * x-coordinate of right edge
     */
    public final int right;

    /**
     * y-coordinate of bottom edge
     */
    public final int bottom;

    /**
     * y-coordinate of top edge
     */
    public final int top;

    public Area(int left, int right, int bottom, int top) {
        if (left > right) {
            throw new IllegalArgumentException("left must less than right");
        }
        if (bottom > top) {
            throw new IllegalArgumentException("bottom must less than top");
        }
        this.left = left;
        this.right = right;
        this.bottom = bottom;
        this.top = top;
    }
}
