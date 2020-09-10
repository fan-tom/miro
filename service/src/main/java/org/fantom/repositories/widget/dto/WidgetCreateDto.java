package org.fantom.repositories.widget.dto;

import org.fantom.domain.Widget;

import java.util.Date;

/**
 * Immutable dto to create widget
 */
public final class WidgetCreateDto {
    public final int x;
    public final int y;
    public final int zIndex;
    public final int width;
    public final int height;
    public final Date updatedAt;

    public WidgetCreateDto(int x, int y, int zIndex, int width, int height, Date updatedAt) {
        this.x = x;
        this.y = y;
        this.zIndex = zIndex;
        this.width = width;
        this.height = height;
        this.updatedAt = updatedAt;
    }

    public <ID> Widget<ID> toWidget(ID id) {
        return new Widget<>(id, x, y, zIndex, width, height, updatedAt);
    }
}
