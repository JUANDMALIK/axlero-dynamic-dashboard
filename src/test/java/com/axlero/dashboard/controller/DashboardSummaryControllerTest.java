package com.axlero.dashboard.controller;

import com.axlero.dashboard.service.DashboardExportService;
import com.axlero.dashboard.service.DashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DashboardSummaryControllerTest {

    @Test
    void summaryUsesAuthenticatedPrincipalAndIsAvailableAtDashboardRoute() throws Exception {
        DashboardService dashboardService = mock(DashboardService.class);
        DashboardSummaryController controller = new DashboardSummaryController(
                dashboardService, mock(DashboardExportService.class));
        var authentication = new UsernamePasswordAuthenticationToken(
                "alice", "n/a");
        Map<String, Object> summary = Map.of("dashboardName", "Alice's dashboard");
        when(dashboardService.getDashboardSummary("alice")).thenReturn(summary);

        assertEquals(summary, controller.getDashboard(authentication));
        verify(dashboardService).getDashboardSummary("alice");

        Method route = DashboardSummaryController.class.getMethod(
                "getDashboard", org.springframework.security.core.Authentication.class);
        assertEquals("/dashboard", route.getAnnotation(GetMapping.class).value()[0]);
    }
}
