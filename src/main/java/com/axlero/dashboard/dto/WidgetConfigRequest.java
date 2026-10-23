package com.axlero.dashboard.dto;

import jakarta.validation.constraints.NotBlank;

public record WidgetConfigRequest(
        @NotBlank String title,
        String type,
        String dataKey
) {}
