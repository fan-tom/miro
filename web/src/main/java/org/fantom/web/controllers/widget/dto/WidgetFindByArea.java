package org.fantom.web.controllers.widget.dto;

import javax.validation.constraints.NotNull;

public class WidgetFindByArea {
    @NotNull
    public final int left;

    @NotNull
    public final int right;

    @NotNull
    public final int bottom;

    @NotNull
    public final int top;

    public WidgetFindByArea(int left, int right, int bottom, int top) {
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
