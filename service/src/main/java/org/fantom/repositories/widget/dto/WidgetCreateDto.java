package org.fantom.repositories.widget.dto;

import org.fantom.domain.Widget;

import java.util.Date;

/**
 * Immutable dto to create widget
 */
public final class WidgetCreateDto {
    /**
     * x-coordinate of left-bottom vertex
     */
    public final int x;

    /**
     * y-coordinate of left-bottom vertex
     */
    public final int y;

    /**
     * z-coordinate of widget's plane, unique among all widgets, regardless of their coordinates
     */
    public final int zIndex;

    /**
     * Widget width
     */
    public final int width;

    /**
     * Widget height
     */
    public final int height;

    /**
     * Widget's create or last modification date
     */
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
