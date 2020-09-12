package org.fantom.repository;

import org.fantom.domain.Widget;

import java.util.Date;
import java.util.Objects;

public final class WidgetDao<ID> {
    /**
     * Widget identifier, unique among all widgets
     */
    public final ID id;
    /**
     * x-coordinate of left-bottom vertex
     */
    public final int x;
    /**
     * y-coordinate of left-bottom vertex
     */
    public final int y;
    /**
     * Widget width
     */
    public final int width;
    /**
     * Widget height
     */
    public final int height;
    /**
     * z-coordinate of widget's plane, unique among all widgets, regardless of their coordinates
     */
    public int zIndex;
    /**
     * Widget's create or last modification date
     */
    public final Date updatedAt;

    public WidgetDao(Widget<ID> widget) {
        this.id = widget.id;
        this.x = widget.x;
        this.y = widget.y;
        this.zIndex = widget.zIndex;
        this.width = widget.width;
        this.height = widget.height;
        this.updatedAt = widget.updatedAt;
    }

    public Widget<ID> toWidget() {
        return new Widget<>(id, x, y, zIndex, width, height, updatedAt);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o == null || getClass() != o.getClass()) {
            return false;
        } else {
            var widget = (WidgetDao<?>) o;
            return id.equals(widget.id);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
