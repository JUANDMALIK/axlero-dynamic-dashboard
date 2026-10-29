package com.axlero.dashboard.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record DashboardRequest(
        @NotBlank @Size(max = 150) String name,
        @NotNull @Size(max = 20) List<@Valid WidgetConfigRequest> widgets) {
}
