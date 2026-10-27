package com.axlero.dashboard.service;

import com.axlero.dashboard.dto.WidgetRequest;
import com.axlero.dashboard.entity.Dashboard;
import com.axlero.dashboard.entity.User;
import com.axlero.dashboard.entity.WidgetConfig;
import com.axlero.dashboard.repository.DashboardRepository;
import com.axlero.dashboard.repository.UserRepository;
import com.axlero.dashboard.repository.WidgetConfigRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private DashboardRepository dashboardRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WidgetConfigRepository widgetConfigRepository;

    @InjectMocks
    private DashboardService dashboardService;

    private Dashboard dashboard;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "alice",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        User user = new User();
        user.setId(12L);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        dashboard = new Dashboard();
        ReflectionTestUtils.setField(dashboard, "id", 34L);
        when(dashboardRepository.findFirstByCreatedByOrderByIdAsc(12L))
                .thenReturn(Optional.of(dashboard));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createWidgetTrimsFieldsAndAssignsNextPosition() {
        when(widgetConfigRepository.findByDashboardIdOrderByPositionAscIdAsc(34L))
                .thenReturn(List.of());
        when(widgetConfigRepository.save(any(WidgetConfig.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = dashboardService.createWidget(
                new WidgetRequest("  Revenue  ", "BAR", "SUM", "  month ", " amount "));

        assertEquals("Revenue", result.title());
        assertEquals("amount", result.valueField());
        assertEquals("month", result.groupByField());
        assertEquals(0, result.position());
    }

    @Test
    void reorderWidgetsPersistsPositionsInRequestedOrder() {
        WidgetConfig first = widget(1L, 0);
        WidgetConfig second = widget(2L, 1);
        when(widgetConfigRepository.findByDashboardIdOrderByPositionAscIdAsc(34L))
                .thenReturn(List.of(first, second));

        var result = dashboardService.updateWidgetOrder(List.of(2L, 1L));

        assertEquals(List.of(2L, 1L), result.stream().map(item -> item.id()).toList());
        assertEquals(0, second.getPosition());
        assertEquals(1, first.getPosition());
        verify(widgetConfigRepository).saveAll(any());
    }

    @Test
    void reorderWidgetsRejectsMissingIds() {
        when(widgetConfigRepository.findByDashboardIdOrderByPositionAscIdAsc(34L))
                .thenReturn(List.of(widget(1L, 0), widget(2L, 1)));

        assertThrows(
                ResponseStatusException.class,
                () -> dashboardService.updateWidgetOrder(List.of(1L)));
        verify(widgetConfigRepository, never()).saveAll(any());
    }

    private WidgetConfig widget(Long id, int position) {
        WidgetConfig widget = new WidgetConfig();
        ReflectionTestUtils.setField(widget, "id", id);
        widget.setTitle("Widget " + id);
        widget.setChartType("BAR");
        widget.setMetricType("COUNT");
        widget.setValueField("id");
        widget.setPosition(position);
        widget.setDashboard(dashboard);
        return widget;
    }
}
