package com.axlero.dashboard.repository;

import com.axlero.dashboard.entity.Dashboard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DashboardRepository extends JpaRepository<Dashboard, Long> {
    Optional<Dashboard> findFirstByCreatedByOrderByIdAsc(Long createdBy);
}
