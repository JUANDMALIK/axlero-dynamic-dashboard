package com.axlero.dashboard.dto;

import java.time.LocalDateTime;

public record WorkspaceResponse(
        Long id,
        String name,
        String description,
        LocalDateTime createdAt
) {
}