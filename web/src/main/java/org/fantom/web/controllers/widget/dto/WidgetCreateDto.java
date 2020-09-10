package org.fantom.web.controllers.widget.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

public class WidgetCreateDto {
    @NotNull
    public final int x;

    @NotNull
    public final int y;

    public final Integer zIndex;

    @NotNull
    @Positive
    public final int width;

    @NotNull
    @Positive
    public final int height;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public WidgetCreateDto(
            @JsonProperty(value = "x", required = true) int x,
            @JsonProperty(value = "y", required = true) int y,
            @JsonProperty(value = "zIndex") Integer zIndex,
            @JsonProperty(value = "width", required = true) int width,
            @JsonProperty(value = "height", required = true) int height
    ) {
        this.x = x;
        this.y = y;
        this.zIndex = zIndex;
        this.width = width;
        this.height = height;
    }

    public org.fantom.services.widget.dto.WidgetCreateDto toServiceDto() {
        return new org.fantom.services.widget.dto.WidgetCreateDto(x, y, zIndex, width, height);
    }
}
