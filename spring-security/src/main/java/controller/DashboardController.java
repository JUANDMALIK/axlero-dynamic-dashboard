

package controller;

import entity.WidgetConfig;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import service.DashboardService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    // Existing dashboard data endpoint
    @GetMapping("/dashboard")
    public Map<String, Object> getDashboard() {
        Map<String, Object> data = new HashMap<>();

        data.put("totalUsers", 150);
        data.put("totalSales", 25000);
        data.put("totalOrders", 75);
        data.put("message", "Dashboard data fetched successfully");

        return data;
    }

    // Load the current user's saved widgets
    @GetMapping("/api/dashboard/widgets")
    public ResponseEntity<List<WidgetConfig>> getWidgets() {
        return ResponseEntity.ok(dashboardService.getWidgets());
    }

    // Save the current user's widget configuration
    @PutMapping("/api/dashboard/widgets")
    public ResponseEntity<List<WidgetConfig>> saveWidgets(
            @RequestBody List<WidgetConfig> widgets) {
        return ResponseEntity.ok(dashboardService.saveWidgets(widgets));
    }
}
