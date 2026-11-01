package com.axlero.dashboard.service;

import com.axlero.dashboard.dto.WidgetConfigRequest;
import com.axlero.dashboard.entity.Dashboard;
import com.axlero.dashboard.entity.WidgetConfig;
import com.axlero.dashboard.repository.WidgetConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WidgetConfigServiceTest {

    @Mock
    private DashboardService dashboardService;

    @Mock
    private WidgetConfigRepository widgetRepository;

    @InjectMocks
    private WidgetConfigService widgetConfigService;

    private Dashboard dashboard;

    @BeforeEach
    void setUp() {
        dashboard = new Dashboard();
        ReflectionTestUtils.setField(dashboard, "id", 42L);
        when(dashboardService.getOrCreateDashboard("alice")).thenReturn(dashboard);
    }

    @Test
    void legacyReplacementDoesNotDeleteChartBuilderWidgets() {
        WidgetConfig chartWidget = widget("BAR", "SUM");
        WidgetConfig legacyWidget = widget("stat", "VALUE");
        when(widgetRepository.findByDashboardIdOrderByPositionAscIdAsc(42L))
                .thenReturn(List.of(chartWidget, legacyWidget));
        when(widgetRepository.save(any(WidgetConfig.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = widgetConfigService.replace(
                "alice", List.of(new WidgetConfigRequest("Legacy", "stat", "totalUsers")));

        assertEquals(1, result.size());
        verify(widgetRepository).deleteAll(List.of(legacyWidget));
        verify(widgetRepository, never()).deleteByDashboardId(42L);
        assertEquals("BAR", chartWidget.getChartType());
        assertEquals("SUM", chartWidget.getMetricType());
    }

    @Test
    void legacyReplacementContinuesToReplaceLegacyWidgets() {
        WidgetConfig legacyWidget = widget("stat", "VALUE");
        when(widgetRepository.findByDashboardIdOrderByPositionAscIdAsc(42L))
                .thenReturn(List.of(legacyWidget));
        when(widgetRepository.save(any(WidgetConfig.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = widgetConfigService.replace(
                "alice", List.of(new WidgetConfigRequest("Users", "stat", "totalUsers")));

        assertEquals(1, result.size());
        assertEquals("Users", result.getFirst().title());
        verify(widgetRepository).deleteAll(List.of(legacyWidget));
        verify(widgetRepository).save(any(WidgetConfig.class));
    }

    private WidgetConfig widget(String chartType, String metricType) {
        WidgetConfig widget = new WidgetConfig();
        widget.setChartType(chartType);
        widget.setMetricType(metricType);
        widget.setDashboard(dashboard);
        return widget;
    }
}
