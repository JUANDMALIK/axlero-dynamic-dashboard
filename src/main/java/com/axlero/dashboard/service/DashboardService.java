
package com.axlero.dashboard.service;

import com.axlero.dashboard.dto.WidgetRequest;
import com.axlero.dashboard.dto.WidgetResponse;
import com.axlero.dashboard.entity.Dashboard;
import com.axlero.dashboard.entity.User;
import com.axlero.dashboard.entity.WidgetConfig;
import com.axlero.dashboard.repository.DashboardRepository;
import com.axlero.dashboard.repository.UserRepository;
import com.axlero.dashboard.repository.WidgetConfigRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class DashboardService {

    private final DashboardRepository dashboardRepository;
    private final UserRepository userRepository;
    private final WidgetConfigRepository widgetConfigRepository;

    public DashboardService(
            DashboardRepository dashboardRepository,
            UserRepository userRepository,
            WidgetConfigRepository widgetConfigRepository) {
        this.dashboardRepository = dashboardRepository;
        this.userRepository = userRepository;
        this.widgetConfigRepository = widgetConfigRepository;
    }

    @Cacheable(cacheNames = "dashboardSummary", key = "#p0")
    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardSummary(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Authenticated user was not found"));

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalUsers", userRepository.count());
        summary.put("totalSales", 0);
        summary.put("totalOrders", 0);
        summary.put("dashboardName", dashboardRepository
                .findFirstByCreatedByOrderByIdAsc(user.getId())
                .map(Dashboard::getName)
                .orElse("My Dashboard"));
        return summary;
    }

    @Transactional
    public Dashboard getOrCreateDashboard(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Authenticated user was not found"));
        return dashboardRepository.findFirstByCreatedByOrderByIdAsc(user.getId())
                .orElseGet(() -> {
                    Dashboard dashboard = new Dashboard();
                    dashboard.setName("My Dashboard");
                    dashboard.setCreatedBy(user.getId());
                    return dashboardRepository.save(dashboard);
                });
    }

    @Transactional
    public Dashboard getMyDashboard() {
        User user = getCurrentUser();
        return dashboardRepository.findFirstByCreatedByOrderByIdAsc(user.getId())
                .orElseGet(() -> {
                    Dashboard dashboard = new Dashboard();
                    dashboard.setName("My Dashboard");
                    dashboard.setCreatedBy(user.getId());
                    return dashboardRepository.save(dashboard);
                });
    }

    @Transactional
    public List<WidgetResponse> getMyWidgets() {
        Long dashboardId = getMyDashboard().getId();
        return widgetConfigRepository
                .findByDashboardIdOrderByPositionAscIdAsc(dashboardId)
                .stream()
                .map(WidgetResponse::from)
                .toList();
    }

    @Transactional
    public WidgetResponse createWidget(WidgetRequest request) {
        Dashboard dashboard = getMyDashboard();
        List<WidgetConfig> existing = widgetConfigRepository
                .findByDashboardIdOrderByPositionAscIdAsc(dashboard.getId())
                .stream()
                .filter(widget -> !isLegacyWidget(widget))
                .toList();

        WidgetConfig widget = new WidgetConfig();
        widget.setTitle(request.title().trim());
        widget.setChartType(request.chartType().trim().toUpperCase());
        widget.setMetricType(request.metricType().trim().toUpperCase());
        widget.setGroupByField(blankToNull(request.groupByField()));
        widget.setValueField(request.valueField().trim());
        widget.setPosition(existing.size());
        widget.setDashboard(dashboard);

        return WidgetResponse.from(widgetConfigRepository.save(widget));
    }

    @Transactional
    public void deleteWidget(Long widgetId) {
        Dashboard dashboard = getMyDashboard();
        WidgetConfig widget = widgetConfigRepository.findById(widgetId)
                .filter(candidate -> !isLegacyWidget(candidate)
                        && candidate.getDashboard().getId().equals(dashboard.getId()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Widget not found"));

        widgetConfigRepository.delete(widget);
        List<WidgetConfig> remaining = widgetConfigRepository
                .findByDashboardIdOrderByPositionAscIdAsc(dashboard.getId());
        List<WidgetConfig> remainingCharts = remaining.stream()
                .filter(candidate -> !isLegacyWidget(candidate))
                .toList();
        IntStream.range(0, remainingCharts.size())
                .forEach(index -> remainingCharts.get(index).setPosition(index));
        widgetConfigRepository.saveAll(remainingCharts);
    }

    @Transactional
    public List<WidgetResponse> updateWidgetOrder(List<Long> widgetIds) {
        Dashboard dashboard = getMyDashboard();
        List<WidgetConfig> allWidgets = widgetConfigRepository
                .findByDashboardIdOrderByPositionAscIdAsc(dashboard.getId());
        List<WidgetConfig> widgets = allWidgets.stream()
                .filter(widget -> !isLegacyWidget(widget))
                .toList();
        Set<Long> requestedIds = new HashSet<>(widgetIds);

        if (requestedIds.size() != widgetIds.size()
                || requestedIds.size() != widgets.size()
                || widgets.stream().anyMatch(widget -> !requestedIds.contains(widget.getId()))) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Order must include every dashboard widget exactly once");
        }

        var widgetsById = widgets.stream()
                .collect(Collectors.toMap(WidgetConfig::getId, widget -> widget));
        List<WidgetConfig> reordered = IntStream.range(0, widgetIds.size())
                .mapToObj(index -> {
                    WidgetConfig widget = widgetsById.get(widgetIds.get(index));
                    widget.setPosition(index);
                    return widget;
                })
                .toList();

        widgetConfigRepository.saveAll(reordered);
        return reordered.stream()
                .map(WidgetResponse::from)
                .toList();
    }

    private User getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || authentication instanceof AnonymousAuthenticationToken
                || "anonymousUser".equals(authentication.getName())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication required");
        }

        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Authenticated user no longer exists"));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private boolean isLegacyWidget(WidgetConfig widget) {
        return "VALUE".equalsIgnoreCase(widget.getMetricType());
    }
}
