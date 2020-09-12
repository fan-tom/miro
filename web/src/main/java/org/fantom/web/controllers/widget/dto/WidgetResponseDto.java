package org.fantom.web.controllers.widget.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import org.fantom.domain.Widget;

import java.util.Date;
import java.util.Objects;

public class WidgetResponseDto<ID> {
    public final ID id;
    public final int x;
    public final int y;
    public final int zIndex;
    public final int width;
    public final int height;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = StdDateFormat.DATE_FORMAT_STR_ISO8601)
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WidgetResponseDto<?> that = (WidgetResponseDto<?>) o;
        return x == that.x &&
                y == that.y &&
                zIndex == that.zIndex &&
                width == that.width &&
                height == that.height &&
                id.equals(that.id) &&
                updatedAt.equals(that.updatedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, x, y, zIndex, width, height, updatedAt);
    }
}
