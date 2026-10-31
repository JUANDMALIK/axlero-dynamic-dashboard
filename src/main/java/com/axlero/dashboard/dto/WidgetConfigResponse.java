package com.axlero.dashboard.dto;

public record WidgetConfigResponse(
        Long id,
        String title,
        String type,
        String dataKey
) {}
