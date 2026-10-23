package com.axlero.dashboard;

import com.axlero.dashboard.entity.Dashboard;
import com.axlero.dashboard.entity.User;
import com.axlero.dashboard.entity.WidgetConfig;
import com.axlero.dashboard.entity.Workspace;
import com.axlero.dashboard.repository.DashboardRepository;
import com.axlero.dashboard.repository.UserRepository;
import com.axlero.dashboard.repository.WidgetConfigRepository;
import com.axlero.dashboard.repository.WorkspaceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class DashboardPersistenceIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private DashboardRepository dashboardRepository;

    @Autowired
    private WidgetConfigRepository widgetConfigRepository;

    @Test
    void persistsUserAndFindsItByEmail() {
        User user = new User();
        user.setName("Ada Lovelace");
        user.setEmail("ada@example.com");
        user.setPassword("encoded-password");

        User savedUser = userRepository.saveAndFlush(user);

        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getCreatedAt()).isNotNull();
        assertThat(userRepository.findByEmail("ada@example.com"))
                .hasValueSatisfying(found -> assertThat(found.getId()).isEqualTo(savedUser.getId()));
        assertThat(userRepository.existsByEmail("ada@example.com")).isTrue();
        assertThat(userRepository.existsByEmail("missing@example.com")).isFalse();
    }

    @Test
    void cascadesWorkspaceFromUserAndFindsItByOwner() {
        User user = new User();
        user.setName("Grace Hopper");
        user.setEmail("grace@example.com");
        user.setPassword("encoded-password");

        Workspace workspace = new Workspace();
        workspace.setName("Engineering");
        user.addWorkspace(workspace);

        User savedUser = userRepository.saveAndFlush(user);
        Workspace savedWorkspace = workspaceRepository.findByOwner_Id(savedUser.getId()).get(0);

        assertThat(savedWorkspace.getCreatedAt()).isNotNull();
        assertThat(savedWorkspace.getOwner().getId()).isEqualTo(savedUser.getId());
    }

    @Test
    void settingWorkspacesMaintainsOwnerAndCascadePersistence() {
        User user = new User();
        user.setName("Katherine Johnson");
        user.setEmail("katherine@example.com");
        user.setPassword("encoded-password");

        Workspace workspace = new Workspace();
        workspace.setName("Flight Dynamics");
        user.setWorkspaces(List.of(workspace));

        User savedUser = userRepository.saveAndFlush(user);

        assertThat(workspaceRepository.findByOwner_Id(savedUser.getId()))
                .singleElement()
                .satisfies(savedWorkspace -> {
                    assertThat(savedWorkspace.getName()).isEqualTo("Flight Dynamics");
                    assertThat(savedWorkspace.getOwner().getId()).isEqualTo(savedUser.getId());
                });
    }

    @Test
    void persistsDashboardAndFindsWidgetConfigurationsByDashboard() {
        Dashboard dashboard = new Dashboard();
        dashboard.setName("Revenue");
        Dashboard savedDashboard = dashboardRepository.saveAndFlush(dashboard);

        WidgetConfig widget = new WidgetConfig();
        widget.setTitle("Monthly revenue");
        widget.setChartType("bar");
        widget.setMetricType("sum");
        widget.setValueField("amount");
        widget.setPosition(0);
        widget.setDashboard(savedDashboard);
        widgetConfigRepository.saveAndFlush(widget);

        assertThat(savedDashboard.getCreatedAt()).isNotNull();
        assertThat(savedDashboard.getUpdatedAt()).isNotNull();
        assertThat(widgetConfigRepository.findByDashboardId(savedDashboard.getId()))
                .singleElement()
                .satisfies(found -> {
                    assertThat(found.getTitle()).isEqualTo("Monthly revenue");
                    assertThat(found.getDashboard().getId()).isEqualTo(savedDashboard.getId());
                });
    }

    @Test
    void newDashboardTimestampsAreInitializedTogether() {
        Dashboard dashboard = new Dashboard();
        dashboard.setName("Operations");

        Dashboard saved = dashboardRepository.saveAndFlush(dashboard);

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isAfterOrEqualTo(saved.getCreatedAt());
    }
}
