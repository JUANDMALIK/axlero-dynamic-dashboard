package com.axlero.dashboard.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record WidgetOrderRequest(
        @NotEmpty List<@NotNull Long> widgetIds
) {
}
