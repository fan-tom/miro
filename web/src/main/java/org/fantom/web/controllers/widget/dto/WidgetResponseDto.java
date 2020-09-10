package org.fantom.web.controllers.widget.dto;

import org.fantom.domain.Widget;

import java.util.Date;

public class WidgetResponseDto<ID> {
    public final ID id;
    public final int x;
    public final int y;
    public final int zIndex;
    public final int width;
    public final int height;
    public final Date updatedAt;

    public WidgetResponseDto(
            ID id,
            int x,
            int y,
            Integer zIndex,
            int width,
            int height,
            Date updatedAt) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.zIndex = zIndex;
        this.width = width;
        this.height = height;
        this.updatedAt = updatedAt;
    }

    public static <ID> WidgetResponseDto<ID> fromWidget(Widget<ID> widget) {
        return new WidgetResponseDto<>(widget.id, widget.x, widget.y, widget.zIndex, widget.width, widget.height, widget.updatedAt);
    }
}
