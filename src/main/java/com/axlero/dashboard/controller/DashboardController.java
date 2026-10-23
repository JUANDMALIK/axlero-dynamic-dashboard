package com.axlero.dashboard.controller;

import com.axlero.dashboard.dto.DashboardCreateRequest;
import com.axlero.dashboard.dto.DashboardResponse;
import com.axlero.dashboard.service.DashboardService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @PostMapping("/api/workspaces/{workspaceId}/dashboards")
    @ResponseStatus(HttpStatus.CREATED)
    public DashboardResponse create(
            @PathVariable Long workspaceId,
            @Valid @RequestBody DashboardCreateRequest request,
            Authentication authentication) {
        return dashboardService.create(
                authentication.getName(), workspaceId, request);
    }

    @GetMapping("/api/workspaces/{workspaceId}/dashboards")
    public List<DashboardResponse> list(
            @PathVariable Long workspaceId,
            Authentication authentication) {
        return dashboardService.list(authentication.getName(), workspaceId);
    }

    @GetMapping("/api/dashboards/{dashboardId}")
    public DashboardResponse get(
            @PathVariable Long dashboardId,
            Authentication authentication) {
        return dashboardService.get(authentication.getName(), dashboardId);
    }

    @PutMapping("/api/dashboards/{dashboardId}")
    public DashboardResponse update(
            @PathVariable Long dashboardId,
            @Valid @RequestBody DashboardCreateRequest request,
            Authentication authentication) {
        return dashboardService.update(
                authentication.getName(), dashboardId, request);
    }

    @DeleteMapping("/api/dashboards/{dashboardId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long dashboardId,
            Authentication authentication) {
        dashboardService.delete(authentication.getName(), dashboardId);
    }
}
