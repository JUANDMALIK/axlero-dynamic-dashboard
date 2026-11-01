package com.axlero.dashboard.dto;

import com.axlero.dashboard.entity.WidgetConfig;

public record WidgetResponse(
        Long id,
        String title,
        String chartType,
        String metricType,
        String groupByField,
        String valueField,
        Integer position,
        Long dashboardId,
        String type,
        String dataKey
) {
    public WidgetResponse(
            Long id,
            String title,
            String chartType,
            String metricType,
            String groupByField,
            String valueField,
            Integer position) {
        this(id, title, chartType, metricType, groupByField, valueField, position,
                null, chartType, valueField);
    }

    public WidgetResponse(
            Long id,
            String title,
            String chartType,
            String metricType,
            String groupByField,
            String valueField,
            Integer position,
            Long dashboardId) {
        this(id, title, chartType, metricType, groupByField, valueField, position,
                dashboardId, chartType, valueField);
    }

    public static WidgetResponse from(WidgetConfig widget) {
        return new WidgetResponse(
                widget.getId(),
                widget.getTitle(),
                widget.getChartType(),
                widget.getMetricType(),
                widget.getGroupByField(),
                widget.getValueField(),
                widget.getPosition(),
                widget.getDashboard() == null ? null : widget.getDashboard().getId(),
                widget.getChartType(),
                widget.getValueField()
        );
    }
}
