
package com.axlero.dashboard.repository;

import com.axlero.dashboard.entity.Dashboard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DashboardRepository
        extends JpaRepository<Dashboard, Long> {

    List<Dashboard> findByCreatedByOrderByUpdatedAtDesc(Long createdBy);

    Optional<Dashboard> findByIdAndCreatedBy(Long id, Long createdBy);
}
