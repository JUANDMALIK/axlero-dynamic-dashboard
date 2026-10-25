package com.axlero.dashboard.api;

import com.axlero.dashboard.entity.WidgetConfig;

public record WidgetConfigResponse(
        Long id,
        String title,
        String chartType,
        String metricType,
        String groupByField,
        String valueField,
        Integer position) {

    public static WidgetConfigResponse from(WidgetConfig widget) {
        return new WidgetConfigResponse(
                widget.getId(),
                widget.getTitle(),
                widget.getChartType(),
                widget.getMetricType(),
                widget.getGroupByField(),
                widget.getValueField(),
                widget.getPosition());
    }
}
