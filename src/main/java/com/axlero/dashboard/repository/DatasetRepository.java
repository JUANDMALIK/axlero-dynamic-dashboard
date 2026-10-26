package com.axlero.dashboard.repository;

import com.axlero.dashboard.entity.Dataset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DatasetRepository extends JpaRepository<Dataset, Long> {
    Optional<Dataset> findFirstByDashboardIdOrderByUploadedAtDesc(Long dashboardId);
}
