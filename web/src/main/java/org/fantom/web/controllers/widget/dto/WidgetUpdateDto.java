package org.fantom.web.controllers.widget.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class WidgetUpdateDto {
    public final Integer x;
    public final Integer y;
    public final Integer zIndex;
    public final Integer width;
    public final Integer height;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public WidgetUpdateDto(
            @JsonProperty("x") Integer x,
            @JsonProperty("y") Integer y,
            @JsonProperty(value = "zIndex") Integer zIndex,
            @JsonProperty("width") Integer width,
            @JsonProperty("height") Integer height
    ) {
        this.x = x;
        this.y = y;
        this.zIndex = zIndex;
        this.width = width;
        this.height = height;
    }

    public <ID> org.fantom.services.widget.dto.WidgetUpdateDto<ID> toServiceDto(ID id) {
        return new org.fantom.services.widget.dto.WidgetUpdateDto<>(id, x, y, zIndex, width, height);
    }
}
