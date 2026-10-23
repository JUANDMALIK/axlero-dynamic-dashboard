package com.axlero.dashboard.repository;

import com.axlero.dashboard.entity.WidgetConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WidgetConfigRepository extends JpaRepository<WidgetConfig, Long> {
    List<WidgetConfig> findByDashboardIdOrderByPositionAsc(Long dashboardId);
    void deleteByDashboardId(Long dashboardId);
}
