package com.axlero.dashboard.repository;

import com.axlero.dashboard.entity.DashboardDataset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DashboardDatasetRepository
        extends JpaRepository<DashboardDataset, Long> {

    Optional<DashboardDataset> findByDashboardId(Long dashboardId);
}
