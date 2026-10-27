package com.axlero.dashboard.dto;

import com.axlero.dashboard.entity.WidgetConfig;

public record WidgetResponse(
        Long id,
        String title,
        String chartType,
        String metricType,
        String groupByField,
        String valueField,
        Integer position
) {
    public static WidgetResponse from(WidgetConfig widget) {
        return new WidgetResponse(
                widget.getId(),
                widget.getTitle(),
                widget.getChartType(),
                widget.getMetricType(),
                widget.getGroupByField(),
                widget.getValueField(),
                widget.getPosition()
        );
    }
}
