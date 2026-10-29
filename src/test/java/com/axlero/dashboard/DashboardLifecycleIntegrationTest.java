package com.axlero.dashboard;

import com.axlero.dashboard.entity.Dashboard;
import com.axlero.dashboard.repository.DashboardRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class DashboardLifecycleIntegrationTest {

    @Autowired
    private DashboardRepository dashboardRepository;

    @Test
    void updatesUpdatedAtWhenDashboardChanges() throws InterruptedException {
        Dashboard dashboard = new Dashboard();
        dashboard.setName("Operations");
        Dashboard saved = dashboardRepository.saveAndFlush(dashboard);
        LocalDateTime initialCreatedAt = saved.getCreatedAt();
        LocalDateTime initialUpdatedAt = saved.getUpdatedAt();

        Thread.sleep(25);

        saved.setName("Operations overview");
        Dashboard updated = dashboardRepository.saveAndFlush(saved);

        assertThat(updated.getCreatedAt()).isEqualTo(initialCreatedAt);
        assertThat(updated.getUpdatedAt()).isAfter(initialUpdatedAt);
    }
}
