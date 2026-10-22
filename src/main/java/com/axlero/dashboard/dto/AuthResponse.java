package com.axlero.dashboard.dto;

public record AuthResponse(
    String token,
    String tokenType,
    String username
) {}