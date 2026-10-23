package com.axlero.dashboard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record WidgetCreateRequest(
        @NotBlank @Size(max = 150) String title,
        @NotBlank @Size(max = 50) String chartType,
        @NotBlank @Size(max = 50) String metricType,
        @Size(max = 100) String groupByField,
        @NotBlank @Size(max = 100) String valueField,
        @NotNull @PositiveOrZero Integer position
) {
}
