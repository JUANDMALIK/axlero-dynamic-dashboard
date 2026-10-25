package com.axlero.dashboard.api;

import com.axlero.dashboard.service.DashboardService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboards")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public List<DashboardResponse> list(Authentication authentication) {
        return dashboardService.list(authentication.getName());
    }

    @GetMapping("/{id}")
    public DashboardResponse get(@PathVariable Long id, Authentication authentication) {
        return dashboardService.get(id, authentication.getName());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DashboardResponse create(
            @Valid @RequestBody DashboardRequest request,
            Authentication authentication) {
        return dashboardService.create(request, authentication.getName());
    }

    @PutMapping("/{id}")
    public DashboardResponse update(
            @PathVariable Long id,
            @Valid @RequestBody DashboardRequest request,
            Authentication authentication) {
        return dashboardService.update(id, request, authentication.getName());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Authentication authentication) {
        dashboardService.delete(id, authentication.getName());
    }
}
