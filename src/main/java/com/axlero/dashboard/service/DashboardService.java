package com.axlero.dashboard.service;

import com.axlero.dashboard.entity.Dashboard;
import com.axlero.dashboard.entity.User;
import com.axlero.dashboard.repository.DashboardRepository;
import com.axlero.dashboard.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class DashboardService {
    private final DashboardRepository dashboardRepository;
    private final UserRepository userRepository;

    public DashboardService(DashboardRepository dashboardRepository,
                            UserRepository userRepository) {
        this.dashboardRepository = dashboardRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardSummary(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user was not found"));

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalUsers", userRepository.count());
        // No sales/order source tables exist in this project yet.
        summary.put("totalSales", 0);
        summary.put("totalOrders", 0);
        summary.put("dashboardName", "My Dashboard");
        return summary;
    }

    @Transactional
    public Dashboard getOrCreateDashboard(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user was not found"));
        return dashboardRepository.findFirstByCreatedByOrderByIdAsc(user.getId())
                .orElseGet(() -> {
                    Dashboard dashboard = new Dashboard();
                    dashboard.setName("My Dashboard");
                    dashboard.setCreatedBy(user.getId());
                    return dashboardRepository.save(dashboard);
                });
    }
}
