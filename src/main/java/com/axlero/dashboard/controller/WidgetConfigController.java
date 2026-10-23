package com.axlero.dashboard.controller;

import com.axlero.dashboard.dto.WidgetCreateRequest;
import com.axlero.dashboard.dto.WidgetResponse;
import com.axlero.dashboard.service.WidgetConfigService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class WidgetConfigController {

    private final WidgetConfigService widgetService;

    public WidgetConfigController(WidgetConfigService widgetService) {
        this.widgetService = widgetService;
    }

    @PostMapping("/dashboards/{dashboardId}/widgets")
    public ResponseEntity<WidgetResponse> create(
            @PathVariable Long dashboardId,
            @Valid @RequestBody WidgetCreateRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(widgetService.create(dashboardId, request, authentication));
    }

    @GetMapping("/dashboards/{dashboardId}/widgets")
    public List<WidgetResponse> list(
            @PathVariable Long dashboardId,
            Authentication authentication) {
        return widgetService.list(dashboardId, authentication);
    }

    @PutMapping("/widgets/{widgetId}")
    public WidgetResponse update(
            @PathVariable Long widgetId,
            @Valid @RequestBody WidgetCreateRequest request,
            Authentication authentication) {
        return widgetService.update(widgetId, request, authentication);
    }

    @DeleteMapping("/widgets/{widgetId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long widgetId,
            Authentication authentication) {
        widgetService.delete(widgetId, authentication);
        return ResponseEntity.noContent().build();
    }
}