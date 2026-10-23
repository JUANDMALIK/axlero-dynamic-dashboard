package com.axlero.dashboard.dto;

import java.time.LocalDateTime;

public record DashboardResponse(
        Long id,
        String name,
        Long workspaceId,
        Long createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
