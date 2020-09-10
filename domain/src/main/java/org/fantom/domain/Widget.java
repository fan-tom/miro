package org.fantom.domain;

import java.util.Date;
import java.util.Objects;

/** Widget that represents rectangular region on plane
 * @param <ID> type of widget id
 */
public final class Widget<ID> {

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

    /**
     *
     * @param id widget identifier
     * @param x x-coordinate of bottom-left vertex
     * @param y y-coordinate of bottom-left vertex
     * @param zIndex z-coordinate of widget plane
     * @param width widget's width (difference between x-coordinates of right and left vertices
     * @param height widget's height (difference between y-coordinates of top and bottom vertices
     * @param updatedAt date of last modification
     * @throws IllegalArgumentException when width or height are negative
     */
    public Widget(ID id, int x, int y, int zIndex, int width, int height, Date updatedAt) throws IllegalArgumentException {
        // TODO: allow zero width/height?
        if (width < 0) {
            throw new IllegalArgumentException("Widget width must be non-negative");
        }
        if (height < 0) {
            throw new IllegalArgumentException("Widget height must be non-negative");
        }
        this.id = id;
        this.x = x;
        this.y = y;
        this.zIndex = zIndex;
        this.width = width;
        this.height = height;
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o == null || getClass() != o.getClass()) {
            return false;
        } else {
            Widget<?> widget = (Widget<?>) o;
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

    public static class Builder<ID> {
        private final Widget<ID> orig;
        private ID id;
        private Integer x;
        private Integer y;
        private Integer width;
        private Integer height;
        private Integer zIndex;
        private Date updatedAt;

        public Builder(Widget<ID> orig) {
            this.orig = orig;
        }

        public Builder<ID> withId(ID id) {
            this.id = id;
            return this;
        }

        public Builder<ID> withX(Integer x) {
            this.x = x;
            return this;
        }

        public Builder<ID> withY(Integer y) {
            this.y = y;
            return this;
        }

        public Builder<ID> withWidth(Integer width) {
            this.width = width;
            return this;
        }

        public Builder<ID> withHeight(Integer height) {
            this.height = height;
            return this;
        }

        public Builder<ID> withZIndex(Integer zIndex) {
            this.zIndex = zIndex;
            return this;
        }

        public Builder<ID> withUpdatedAt(Date updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Widget<ID> build() {
            return new Widget<>(
                    id == null ? orig.id : id,
                    x == null ? orig.x : x,
                    y == null ? orig.y : y,
                    zIndex == null ? orig.zIndex : zIndex, width == null ? orig.width : width,
                    height == null ? orig.height : height,
                    updatedAt == null ? orig.updatedAt : updatedAt
            );
        }
    }
}
