package com.axlero.dashboard.service;

import com.axlero.dashboard.dto.WidgetConfigRequest;
import com.axlero.dashboard.dto.WidgetConfigResponse;
import com.axlero.dashboard.entity.Dashboard;
import com.axlero.dashboard.entity.WidgetConfig;
import com.axlero.dashboard.repository.WidgetConfigRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class WidgetConfigService {
    private final DashboardService dashboardService;
    private final WidgetConfigRepository widgetRepository;

    public WidgetConfigService(DashboardService dashboardService,
                               WidgetConfigRepository widgetRepository) {
        this.dashboardService = dashboardService;
        this.widgetRepository = widgetRepository;
    }

    @Transactional
    public List<WidgetConfigResponse> list(String username) {
        Dashboard dashboard = dashboardService.getOrCreateDashboard(username);
        return widgetRepository.findByDashboardIdOrderByPositionAsc(dashboard.getId())
                .stream()
                .filter(WidgetConfigService::isLegacyWidget)
                .map(WidgetConfigService::toResponse)
                .toList();
    }

    @Transactional
    public List<WidgetConfigResponse> replace(String username,
                                             List<@Valid WidgetConfigRequest> requests) {
        if (requests == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Widget list is required");
        }
        if (requests.stream().anyMatch(request ->
                request == null || request.title() == null || request.title().isBlank())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Every widget must have a title");
        }

        Dashboard dashboard = dashboardService.getOrCreateDashboard(username);
        List<WidgetConfig> existing =
                widgetRepository.findByDashboardIdOrderByPositionAscIdAsc(dashboard.getId());
        List<WidgetConfig> legacyWidgets =
                existing.stream().filter(WidgetConfigService::isLegacyWidget).toList();
        if (!legacyWidgets.isEmpty()) {
            widgetRepository.deleteAll(legacyWidgets);
        }

        List<WidgetConfig> saved = new java.util.ArrayList<>();
        int position = 0;
        for (WidgetConfigRequest request : requests) {
            WidgetConfig widget = new WidgetConfig();
            widget.setTitle(request.title().trim());
            widget.setChartType(blankToDefault(request.type(), "stat"));
            widget.setMetricType("VALUE");
            widget.setValueField(blankToDefault(request.dataKey(), "totalUsers"));
            widget.setPosition(position++);
            widget.setDashboard(dashboard);
            saved.add(widgetRepository.save(widget));
        }
        return saved.stream().map(WidgetConfigService::toResponse).toList();
    }

    private static boolean isLegacyWidget(WidgetConfig widget) {
        return "VALUE".equalsIgnoreCase(widget.getMetricType());
    }

    private static WidgetConfigResponse toResponse(WidgetConfig widget) {
        return new WidgetConfigResponse(widget.getId(), widget.getTitle(),
                widget.getChartType(), widget.getValueField());
    }

    private static String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
