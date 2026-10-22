package com.axlero.dashboard.repository;

import com.axlero.dashboard.entity.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {

    List<Workspace> findByOwnerId(Long ownerId);
}
