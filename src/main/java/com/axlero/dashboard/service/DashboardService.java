package com.axlero.dashboard.service;

import com.axlero.dashboard.api.DashboardRequest;
import com.axlero.dashboard.api.DashboardResponse;
import com.axlero.dashboard.api.WidgetConfigRequest;
import com.axlero.dashboard.api.WidgetConfigResponse;
import com.axlero.dashboard.auth.AuthService;
import com.axlero.dashboard.entity.Dashboard;
import com.axlero.dashboard.entity.User;
import com.axlero.dashboard.entity.WidgetConfig;
import com.axlero.dashboard.repository.DashboardRepository;
import com.axlero.dashboard.repository.UserRepository;
import com.axlero.dashboard.repository.WidgetConfigRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class DashboardService {

    private final DashboardRepository dashboardRepository;
    private final WidgetConfigRepository widgetConfigRepository;
    private final UserRepository userRepository;

    public DashboardService(
            DashboardRepository dashboardRepository,
            WidgetConfigRepository widgetConfigRepository,
            UserRepository userRepository) {
        this.dashboardRepository = dashboardRepository;
        this.widgetConfigRepository = widgetConfigRepository;
        this.userRepository = userRepository;
    }

    @Cacheable(cacheNames = "dashboardSummary", key = "#p0")
    @Transactional(readOnly = true)
    public List<DashboardResponse> list(String email) {
        Long userId = userId(email);
        return dashboardRepository.findByCreatedByOrderByUpdatedAtDesc(userId)
                .stream()
                .map(this::response)
                .toList();
    }

    @Cacheable(cacheNames = "dashboardSummary", key = "#p0 + ':' + #p1")
    @Transactional(readOnly = true)
    public DashboardResponse get(Long dashboardId, String email) {
        return response(findOwnedDashboard(dashboardId, email));
    }

    @CacheEvict(cacheNames = "dashboardSummary", allEntries = true)
    @Transactional
    public DashboardResponse create(DashboardRequest request, String email) {
        Dashboard dashboard = new Dashboard();
        dashboard.setName(request.name().trim());
        dashboard.setCreatedBy(userId(email));
        Dashboard saved = dashboardRepository.save(dashboard);
        return saveWidgets(saved, request.widgets());
    }

    @CacheEvict(cacheNames = "dashboardSummary", allEntries = true)
    @Transactional
    public DashboardResponse update(Long dashboardId, DashboardRequest request, String email) {
        Dashboard dashboard = findOwnedDashboard(dashboardId, email);
        dashboard.setName(request.name().trim());
        Dashboard saved = dashboardRepository.save(dashboard);
        widgetConfigRepository.deleteByDashboardId(saved.getId());
        return saveWidgets(saved, request.widgets());
    }

    @CacheEvict(cacheNames = "dashboardSummary", allEntries = true)
    @Transactional
    public void delete(Long dashboardId, String email) {
        Dashboard dashboard = findOwnedDashboard(dashboardId, email);
        widgetConfigRepository.deleteByDashboardId(dashboard.getId());
        dashboardRepository.delete(dashboard);
    }

    private DashboardResponse saveWidgets(Dashboard dashboard, List<WidgetConfigRequest> widgets) {
        List<WidgetConfig> savedWidgets = widgets.stream()
                .map(widget -> toEntity(widget, dashboard))
                .toList();
        List<WidgetConfigResponse> responses = widgetConfigRepository.saveAll(savedWidgets)
                .stream()
                .map(WidgetConfigResponse::from)
                .toList();
        return DashboardResponse.from(dashboard, responses);
    }

    private WidgetConfig toEntity(WidgetConfigRequest request, Dashboard dashboard) {
        WidgetConfig widget = new WidgetConfig();
        widget.setTitle(request.title().trim());
        widget.setChartType(request.chartType());
        widget.setMetricType(request.metricType());
        widget.setGroupByField(request.groupByField());
        widget.setValueField(request.valueField());
        widget.setPosition(request.position());
        widget.setDashboard(dashboard);
        return widget;
    }

    private DashboardResponse response(Dashboard dashboard) {
        List<WidgetConfigResponse> widgets = widgetConfigRepository.findByDashboardIdOrderByPositionAsc(dashboard.getId())
                .stream()
                .map(WidgetConfigResponse::from)
                .toList();
        return DashboardResponse.from(dashboard, widgets);
    }

    private Dashboard findOwnedDashboard(Long dashboardId, String email) {
        return dashboardRepository.findByIdAndCreatedBy(dashboardId, userId(email))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dashboard not found"));
    }

    private Long userId(String email) {
        return userRepository.findByEmailIgnoreCase(AuthService.normalizeEmail(email))
                .map(User::getId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }
}
