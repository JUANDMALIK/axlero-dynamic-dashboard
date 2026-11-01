
package com.axlero.dashboard.controller;

import com.axlero.dashboard.dto.WidgetOrderRequest;
import com.axlero.dashboard.dto.WidgetRequest;
import com.axlero.dashboard.dto.WidgetResponse;
import com.axlero.dashboard.dto.ChartDataPoint;
import com.axlero.dashboard.dto.DatasetResponse;
import com.axlero.dashboard.entity.Dashboard;
import com.axlero.dashboard.service.DashboardService;
import com.axlero.dashboard.service.DatasetService;
import com.axlero.dashboard.service.WidgetExportService;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final DatasetService datasetService;
    private final WidgetExportService widgetExportService;

    public DashboardController(
            DashboardService dashboardService,
            DatasetService datasetService,
            WidgetExportService widgetExportService) {
        this.dashboardService = dashboardService;
        this.datasetService = datasetService;
        this.widgetExportService = widgetExportService;
    }

    @GetMapping
    public Dashboard getMyDashboard() {
        return dashboardService.getMyDashboard();
    }

    @GetMapping("/widgets")
    public List<WidgetResponse> getMyWidgets() {
        return dashboardService.getMyWidgets();
    }

    @GetMapping("/dataset")
    public DatasetResponse getDataset() {
        return datasetService.getDataset();
    }

    @PostMapping("/dataset")
    public DatasetResponse uploadDataset(@RequestParam("file") MultipartFile file) {
        return datasetService.uploadDataset(file);
    }

    @DeleteMapping("/dataset")
    public ResponseEntity<Void> deleteDataset() {
        datasetService.deleteDataset();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/widgets/{widgetId}/data")
    public List<ChartDataPoint> getWidgetData(@PathVariable Long widgetId) {
        return datasetService.getWidgetData(widgetId);
    }

    @PostMapping("/widgets")
    public WidgetResponse createWidget(@Valid @RequestBody WidgetRequest request) {
        return dashboardService.createWidget(request);
    }

    @PutMapping("/widgets/order")
    public List<WidgetResponse> updateWidgetOrder(
            @Valid @RequestBody WidgetOrderRequest request) {
        return dashboardService.updateWidgetOrder(request.widgetIds());
    }

    @DeleteMapping("/widgets/{widgetId}")
    public ResponseEntity<Void> deleteWidget(@PathVariable Long widgetId) {
        dashboardService.deleteWidget(widgetId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/export.csv")
    public ResponseEntity<byte[]> exportCsv() {
        return exportResponse(
                widgetExportService.exportCsv(),
                "text/csv; charset=UTF-8",
                "dashboard-widgets.csv");
    }

    @GetMapping("/export.xlsx")
    public ResponseEntity<byte[]> exportExcel() {
        return exportResponse(
                widgetExportService.exportExcel(),
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "dashboard-widgets.xlsx");
    }

    private ResponseEntity<byte[]> exportResponse(
            byte[] data,
            String contentType,
            String filename) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(filename, StandardCharsets.UTF_8)
                                .build()
                                .toString())
                .body(data);
    }
}
