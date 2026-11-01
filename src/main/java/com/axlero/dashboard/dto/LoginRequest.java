package com.axlero.dashboard.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank String username,
    @NotBlank String password
) {
    public LoginRequest {
        username = username == null ? null : username.trim();
    }
}