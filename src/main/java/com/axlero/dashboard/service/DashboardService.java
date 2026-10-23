package com.axlero.dashboard.service;

import com.axlero.dashboard.dto.DashboardCreateRequest;
import com.axlero.dashboard.dto.DashboardResponse;
import com.axlero.dashboard.entity.Dashboard;
import com.axlero.dashboard.entity.User;
import com.axlero.dashboard.entity.Workspace;
import com.axlero.dashboard.repository.DashboardRepository;
import com.axlero.dashboard.repository.UserRepository;
import com.axlero.dashboard.repository.WidgetConfigRepository;
import com.axlero.dashboard.repository.WorkspaceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class DashboardService {

    private final DashboardRepository dashboardRepository;
    private final WorkspaceRepository workspaceRepository;
    private final UserRepository userRepository;
    private final WidgetConfigRepository widgetConfigRepository;

    public DashboardService(
            DashboardRepository dashboardRepository,
            WorkspaceRepository workspaceRepository,
            UserRepository userRepository,
            WidgetConfigRepository widgetConfigRepository) {
        this.dashboardRepository = dashboardRepository;
        this.workspaceRepository = workspaceRepository;
        this.userRepository = userRepository;
        this.widgetConfigRepository = widgetConfigRepository;
    }

    @Transactional
    public DashboardResponse create(
            String username,
            Long workspaceId,
            DashboardCreateRequest request) {

        User user = findUser(username);
        Workspace workspace = findOwnedWorkspace(workspaceId, user.getId());

        Dashboard dashboard = new Dashboard();
        dashboard.setName(request.name().trim());
        dashboard.setWorkspaceId(workspace.getId());
        dashboard.setCreatedBy(user.getId());

        return toResponse(dashboardRepository.save(dashboard));
    }

    @Transactional(readOnly = true)
    public List<DashboardResponse> list(String username, Long workspaceId) {
        User user = findUser(username);
        findOwnedWorkspace(workspaceId, user.getId());

        return dashboardRepository
                .findByWorkspaceIdOrderByCreatedAtDesc(workspaceId)
                .stream()
                .map(DashboardService::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DashboardResponse get(String username, Long dashboardId) {
        User user = findUser(username);
        Dashboard dashboard = findDashboard(dashboardId);

        findOwnedWorkspace(dashboard.getWorkspaceId(), user.getId());
        return toResponse(dashboard);
    }

    @Transactional
    public DashboardResponse update(
            String username,
            Long dashboardId,
            DashboardCreateRequest request) {

        User user = findUser(username);
        Dashboard dashboard = findDashboard(dashboardId);

        findOwnedWorkspace(dashboard.getWorkspaceId(), user.getId());

        dashboard.setName(request.name().trim());
        return toResponse(dashboardRepository.save(dashboard));
    }

    @Transactional
    public void delete(String username, Long dashboardId) {
        User user = findUser(username);
        Dashboard dashboard = findDashboard(dashboardId);

        findOwnedWorkspace(dashboard.getWorkspaceId(), user.getId());

        // Remove dependent widget configurations before deleting the dashboard.
        widgetConfigRepository.deleteByDashboardId(dashboardId);
        dashboardRepository.delete(dashboard);
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Authenticated user not found"));
    }

    private Workspace findOwnedWorkspace(Long workspaceId, Long userId) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Workspace not found"));

        if (!workspace.getOwner().getId().equals(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Workspace not found");
        }

        return workspace;
    }

    private Dashboard findDashboard(Long dashboardId) {
        return dashboardRepository.findById(dashboardId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Dashboard not found"));
    }

    private static DashboardResponse toResponse(Dashboard dashboard) {
        return new DashboardResponse(
                dashboard.getId(),
                dashboard.getName(),
                dashboard.getWorkspaceId(),
                dashboard.getCreatedBy(),
                dashboard.getCreatedAt(),
                dashboard.getUpdatedAt());
    }
}
