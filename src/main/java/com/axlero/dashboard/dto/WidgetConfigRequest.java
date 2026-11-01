package com.axlero.dashboard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WidgetConfigRequest(
        @NotBlank @Size(max = 150) String title,
        @Size(max = 50) String type,
        @Size(max = 100) String dataKey
) {}
