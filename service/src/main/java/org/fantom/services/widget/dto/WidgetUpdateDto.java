package org.fantom.services.widget.dto;

import org.fantom.domain.Widget;

import java.util.Date;
import java.util.Optional;

public final class WidgetUpdateDto<ID> {
    public final ID id;
    final Integer x;
    final Integer y;
    final Integer width;
    final Integer height;
    final Integer zIndex;

    public Optional<Integer> getX() {
        return Optional.ofNullable(x);
    }

    public Optional<Integer> getY() {
        return Optional.ofNullable(y);
    }

    public Optional<Integer> getWidth() {
        return Optional.ofNullable(width);
    }

    public Optional<Integer> getHeight() {
        return Optional.ofNullable(height);
    }

    public Optional<Integer> getZIndex() {
        return Optional.ofNullable(zIndex);
    }

    public WidgetUpdateDto(ID id, Integer x, Integer y, Integer zIndex, Integer width, Integer height) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.zIndex = zIndex;
    }

    public Widget<ID> apply(Widget<ID> oldWidget, Date updatedAt) {
        return new Widget<>(
                id,
                getX().orElse(oldWidget.x),
                getY().orElse(oldWidget.y),
                getHeight().orElse(oldWidget.height), getZIndex().orElse(oldWidget.zIndex),
                getWidth().orElse(oldWidget.width),
                updatedAt
        );
    }
}
