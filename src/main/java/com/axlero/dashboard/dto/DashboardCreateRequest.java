package com.axlero.dashboard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DashboardCreateRequest(
        @NotBlank(message = "Dashboard name is required")
        @Size(max = 150, message = "Dashboard name must not exceed 150 characters")
        String name
) {
}
