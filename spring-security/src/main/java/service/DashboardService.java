
package service;

import entity.Dashboard;
import entity.User;
import entity.WidgetConfig;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import repository.DashboardRepository;
import repository.UserRepository;

import java.util.List;

@Service
public class DashboardService {

    private final DashboardRepository dashboardRepository;
    private final UserRepository userRepository;

    public DashboardService(
            DashboardRepository dashboardRepository,
            UserRepository userRepository) {
        this.dashboardRepository = dashboardRepository;
        this.userRepository = userRepository;
    }

    private User getCurrentUser() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalStateException("User is not authenticated");
        }

        String email = authentication.getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User not found"));
    }

    @Transactional(readOnly = true)
    public List<WidgetConfig> getWidgets() {
        User user = getCurrentUser();

        return dashboardRepository.findByOwnerId(user.getId())
                .map(Dashboard::getWidgets)
                .orElse(List.of());
    }

    @Transactional
    public List<WidgetConfig> saveWidgets(List<WidgetConfig> widgets) {
        User user = getCurrentUser();

        Dashboard dashboard = dashboardRepository.findByOwnerId(user.getId())
                .orElseGet(() -> {
                    Dashboard newDashboard = new Dashboard("My Dashboard");
                    newDashboard.setOwner(user);
                    return newDashboard;
                });

        dashboard.getWidgets().clear();

        for (WidgetConfig widget : widgets) {
            WidgetConfig savedWidget = new WidgetConfig(
                    widget.getTitle(),
                    widget.getType(),
                    widget.getDataKey()
            );

            dashboard.addWidget(savedWidget);
        }

        return dashboardRepository.save(dashboard).getWidgets();
    }
}
