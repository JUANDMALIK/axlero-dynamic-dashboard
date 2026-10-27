package com.axlero.dashboard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Locale;

public record WidgetRequest(
        @NotBlank @Size(max = 150) String title,
        @NotBlank @Pattern(regexp = "(?i)BAR|LINE|PIE") String chartType,
        @NotBlank @Pattern(regexp = "(?i)COUNT|SUM|AVERAGE") String metricType,
        @Size(max = 100) String groupByField,
        @NotBlank @Size(max = 100) String valueField
) {
    public WidgetRequest {
        title = title == null ? null : title.trim();
        chartType = chartType == null ? null : chartType.trim().toUpperCase(Locale.ROOT);
        metricType = metricType == null ? null : metricType.trim().toUpperCase(Locale.ROOT);
        groupByField = groupByField == null ? null : groupByField.trim();
        valueField = valueField == null ? null : valueField.trim();
    }
}
