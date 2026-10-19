
package com.axlero.dashboard.repository;

import com.axlero.dashboard.entity.Dashboard;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DashboardRepository
        extends JpaRepository<Dashboard, Long> {
}
