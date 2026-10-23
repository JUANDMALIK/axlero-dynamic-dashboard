package com.axlero.dashboard.service;

import com.axlero.dashboard.dto.WidgetCreateRequest;
import com.axlero.dashboard.dto.WidgetResponse;
import com.axlero.dashboard.entity.Dashboard;
import com.axlero.dashboard.entity.User;
import com.axlero.dashboard.entity.WidgetConfig;
import com.axlero.dashboard.entity.Workspace;
import com.axlero.dashboard.repository.DashboardRepository;
import com.axlero.dashboard.repository.UserRepository;
import com.axlero.dashboard.repository.WidgetConfigRepository;
import com.axlero.dashboard.repository.WorkspaceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional
public class WidgetConfigService {

    private final WidgetConfigRepository widgetRepository;
    private final DashboardRepository dashboardRepository;
    private final WorkspaceRepository workspaceRepository;
    private final UserRepository userRepository;

    public WidgetConfigService(
            WidgetConfigRepository widgetRepository,
            DashboardRepository dashboardRepository,
            WorkspaceRepository workspaceRepository,
            UserRepository userRepository) {
        this.widgetRepository = widgetRepository;
        this.dashboardRepository = dashboardRepository;
        this.workspaceRepository = workspaceRepository;
        this.userRepository = userRepository;
    }

    public WidgetResponse create(Long dashboardId, WidgetCreateRequest request,
                                 Authentication authentication) {
        Dashboard dashboard = findOwnedDashboard(dashboardId, authentication);
        WidgetConfig widget = new WidgetConfig();
        applyRequest(widget, request);
        widget.setDashboard(dashboard);
        return toResponse(widgetRepository.save(widget));
    }

    @Transactional(readOnly = true)
    public List<WidgetResponse> list(Long dashboardId, Authentication authentication) {
        findOwnedDashboard(dashboardId, authentication);
        return widgetRepository.findByDashboardIdOrderByPositionAsc(dashboardId)
                .stream().map(this::toResponse).toList();
    }

    public WidgetResponse update(Long widgetId, WidgetCreateRequest request,
                                 Authentication authentication) {
        WidgetConfig widget = findWidget(widgetId);
        findOwnedDashboard(widget.getDashboard().getId(), authentication);
        applyRequest(widget, request);
        return toResponse(widgetRepository.save(widget));
    }

    public void delete(Long widgetId, Authentication authentication) {
        WidgetConfig widget = findWidget(widgetId);
        findOwnedDashboard(widget.getDashboard().getId(), authentication);
        widgetRepository.delete(widget);
    }

    private Dashboard findOwnedDashboard(Long dashboardId, Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "User not found"));

        Dashboard dashboard = dashboardRepository.findById(dashboardId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Dashboard not found"));

        Workspace workspace = workspaceRepository.findById(dashboard.getWorkspaceId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Workspace not found"));

        if (workspace.getOwner() == null
                || !workspace.getOwner().getId().equals(user.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Dashboard not found");
        }

        return dashboard;
    }

    private WidgetConfig findWidget(Long widgetId) {
        return widgetRepository.findById(widgetId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Widget not found"));
    }

    private void applyRequest(WidgetConfig widget, WidgetCreateRequest request) {
        widget.setTitle(request.title());
        widget.setChartType(request.chartType());
        widget.setMetricType(request.metricType());
        widget.setGroupByField(request.groupByField());
        widget.setValueField(request.valueField());
        widget.setPosition(request.position());
    }

    private WidgetResponse toResponse(WidgetConfig widget) {
        return new WidgetResponse(
                widget.getId(),
                widget.getTitle(),
                widget.getChartType(),
                widget.getMetricType(),
                widget.getGroupByField(),
                widget.getValueField(),
                widget.getPosition(),
                widget.getDashboard().getId()
        );
    }
}
