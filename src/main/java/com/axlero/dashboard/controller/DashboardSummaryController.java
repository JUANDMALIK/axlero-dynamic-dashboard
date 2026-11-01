package com.axlero.dashboard.controller;

import com.axlero.dashboard.service.DashboardExportService;
import com.axlero.dashboard.service.DashboardService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class DashboardSummaryController {

    private final DashboardService dashboardService;
    private final DashboardExportService dashboardExportService;

    public DashboardSummaryController(
            DashboardService dashboardService,
            DashboardExportService dashboardExportService) {
        this.dashboardService = dashboardService;
        this.dashboardExportService = dashboardExportService;
    }

    @GetMapping("/dashboard")
    public Map<String, Object> getDashboard(Authentication authentication) {
        return dashboardService.getDashboardSummary(authentication.getName());
    }

    @GetMapping("/dashboard/export/csv")
    public ResponseEntity<byte[]> exportCsv(Authentication authentication) {
        byte[] file = dashboardExportService.exportCsv(authentication.getName());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"dashboard.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(file);
    }

    @GetMapping("/dashboard/export/excel")
    public ResponseEntity<byte[]> exportExcel(Authentication authentication) {
        byte[] file = dashboardExportService.exportExcel(authentication.getName());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"dashboard.xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(file);
    }
}
