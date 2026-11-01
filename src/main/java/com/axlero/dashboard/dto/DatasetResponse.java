package com.axlero.dashboard.dto;

import java.util.List;

public record DatasetResponse(
        String name,
        List<String> columns,
        int rowCount
) {
}
