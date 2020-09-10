package org.fantom.services.widget.dto;

import java.util.Date;
import java.util.Optional;

public final class WidgetCreateDto {
    public final int x;
    public final int y;
    public final int width;
    public final int height;
    public final Integer zIndex;

    public Optional<Integer> getZIndex() {
        return Optional.ofNullable(zIndex);
    }

    public WidgetCreateDto(int x, int y, Integer zIndex, int width, int height) {
        this.x = x;
        this.y = y;
        this.zIndex = zIndex;
        this.width = width;
        this.height = height;
    }

    public org.fantom.repositories.widget.dto.WidgetCreateDto toRepoDto(Date updatedAt) {
        return new org.fantom.repositories.widget.dto.WidgetCreateDto(x, y, zIndex, width, height, updatedAt);
    }
}
