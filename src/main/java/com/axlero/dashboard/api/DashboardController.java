package com.axlero.dashboard.api;

import com.axlero.dashboard.service.DashboardService;
import com.axlero.dashboard.service.DashboardExportService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/dashboards")
@Tag(name = "Dashboards", description = "Create, manage, and export the authenticated user's dashboards")
@SecurityRequirement(name = "sessionCookie")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Invalid request or validation failure",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Request is forbidden or CSRF token is invalid",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Dashboard not found or not owned by the user",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Database constraint conflict",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
})
public class DashboardController {

    private final DashboardService dashboardService;
    private final DashboardExportService dashboardExportService;

    public DashboardController(
            DashboardService dashboardService,
            DashboardExportService dashboardExportService) {
        this.dashboardService = dashboardService;
        this.dashboardExportService = dashboardExportService;
    }

    @GetMapping
    @Operation(summary = "List dashboards", description = "Returns dashboards owned by the authenticated user.")
    public List<DashboardResponse> list(Authentication authentication) {
        return dashboardService.list(authentication.getName());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a dashboard", description = "Returns one dashboard owned by the authenticated user.")
    public DashboardResponse get(@PathVariable Long id, Authentication authentication) {
        return dashboardService.get(id, authentication.getName());
    }

    @GetMapping("/{id}/export")
    @Operation(summary = "Export dashboard configuration",
            description = "Downloads saved dashboard and widget configuration as CSV or Excel (.xlsx).")
    public ResponseEntity<byte[]> export(
            @PathVariable Long id,
            @Parameter(description = "Export format: csv or xlsx", example = "csv")
            @RequestParam String format,
            Authentication authentication) {
        ExportFormat exportFormat;
        try {
            exportFormat = ExportFormat.valueOf(format.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Format must be csv or xlsx");
        }

        DashboardResponse dashboard = dashboardService.get(id, authentication.getName());
        byte[] content = exportFormat == ExportFormat.CSV
                ? dashboardExportService.toCsv(dashboard)
                : dashboardExportService.toExcel(dashboard);
        String extension = exportFormat == ExportFormat.CSV ? "csv" : "xlsx";
        String filename = "dashboard-" + dashboard.id() + "-" + safeFilename(dashboard.name())
                + "." + extension;
        MediaType contentType = exportFormat == ExportFormat.CSV
                ? new MediaType("text", "csv", StandardCharsets.UTF_8)
                : MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

        return ResponseEntity.ok()
                .contentType(contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build().toString())
                .body(content);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a dashboard", description = "Creates a dashboard with its widget configuration.")
    public DashboardResponse create(
            @Valid @RequestBody DashboardRequest request,
            Authentication authentication) {
        return dashboardService.create(request, authentication.getName());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace a dashboard", description = "Replaces the dashboard name and its complete widget list.")
    public DashboardResponse update(
            @PathVariable Long id,
            @Valid @RequestBody DashboardRequest request,
            Authentication authentication) {
        return dashboardService.update(id, request, authentication.getName());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a dashboard", description = "Deletes a dashboard and all of its widget configuration.")
    public void delete(@PathVariable Long id, Authentication authentication) {
        dashboardService.delete(id, authentication.getName());
    }

    private String safeFilename(String name) {
        String filename = name.replaceAll("[^A-Za-z0-9._-]+", "-")
                .replaceAll("^[.-]+|[.-]+$", "");
        return filename.isBlank() ? "dashboard" : filename;
    }
}
