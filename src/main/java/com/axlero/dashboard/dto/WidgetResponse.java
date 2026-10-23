package com.axlero.dashboard.dto;

public record WidgetResponse(
        Long id,
        String title,
        String chartType,
        String metricType,
        String groupByField,
        String valueField,
        Integer position,
        Long dashboardId
) {
}
