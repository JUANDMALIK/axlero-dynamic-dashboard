package com.axlero.dashboard.dto;

public record WorkspaceCreateRequest(
        String name,
        String description
) {
}