package com.axlero.dashboard.repository;

import com.axlero.dashboard.entity.Dashboard;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DashboardRepository extends JpaRepository<Dashboard, Long> {

    List<Dashboard> findByWorkspaceIdOrderByCreatedAtDesc(Long workspaceId);

    Optional<Dashboard> findByIdAndWorkspaceId(Long id, Long workspaceId);

    boolean existsByWorkspaceId(Long workspaceId);
}
