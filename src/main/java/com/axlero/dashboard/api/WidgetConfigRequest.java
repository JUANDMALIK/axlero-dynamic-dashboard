package com.axlero.dashboard.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record WidgetConfigRequest(
        @NotBlank @Size(max = 150) String title,
        @NotBlank @Pattern(regexp = "bar|line|area|pie") String chartType,
        @NotBlank @Pattern(regexp = "sum|average|count") String metricType,
        @Size(max = 100) String groupByField,
        @NotBlank @Size(max = 100) String valueField,
        @NotNull @Min(0) Integer position) {
}
