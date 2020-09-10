package org.fantom.repository;

import org.fantom.domain.Widget;

import java.util.Date;
import java.util.Objects;

public final class WidgetDao<ID> {
    /**
     * Widget identifier, unique among all widgets. Unmodifiable
     */
    public final ID id;
    /**
     * x-coordinate of left-bottom vertex
     */
    public int x;
    /**
     * y-coordinate of left-bottom vertex
     */
    public int y;
    /**
     * Widget width
     */
    public int width;
    /**
     * Widget height
     */
    public int height;
    /**
     * z-coordinate of widget's plane, unique among all widgets, regardless of their coordinates
     */
    public int zIndex;
    /**
     * Widget's create or last modification date
     */
    public Date updatedAt;

    public WidgetDao(ID id, int x, int y, int width, int height, int zIndex, Date updatedAt) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.zIndex = zIndex;
        this.width = width;
        this.height = height;
        this.updatedAt = updatedAt;
    }

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
            return x == widget.x &&
                    y == widget.y &&
                    width == widget.width &&
                    height == widget.height &&
                    zIndex == widget.zIndex &&
                    id.equals(widget.id) &&
                    updatedAt.equals(widget.updatedAt);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, x, y, width, height, zIndex, updatedAt);
    }
}
