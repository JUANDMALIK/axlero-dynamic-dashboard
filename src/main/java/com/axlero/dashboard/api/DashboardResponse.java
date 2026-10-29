package com.axlero.dashboard.api;

import com.axlero.dashboard.entity.Dashboard;

import java.time.LocalDateTime;
import java.util.List;

public record DashboardResponse(
        Long id,
        String name,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<WidgetConfigResponse> widgets) {

    public static DashboardResponse from(Dashboard dashboard, List<WidgetConfigResponse> widgets) {
        return new DashboardResponse(
                dashboard.getId(),
                dashboard.getName(),
                dashboard.getCreatedAt(),
                dashboard.getUpdatedAt(),
                widgets);
    }
}
