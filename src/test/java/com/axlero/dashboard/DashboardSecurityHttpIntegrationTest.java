package com.axlero.dashboard;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:dashboard-http-test;DB_CLOSE_DELAY=-1",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.h2.console.enabled=true"
        })
class DashboardSecurityHttpIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void unauthenticatedDashboardApiRequestIsForbidden() {
        ResponseEntity<String> response =
                restTemplate.getForEntity("/api/dashboards", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void h2ConsoleLoginPageIsAccessibleWithoutAuthentication() {
        ResponseEntity<String> response =
                restTemplate.getForEntity("/h2-console/", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
