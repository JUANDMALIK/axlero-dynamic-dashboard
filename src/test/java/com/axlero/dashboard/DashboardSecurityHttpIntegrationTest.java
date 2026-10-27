package com.axlero.dashboard;

import com.axlero.dashboard.auth.CsrfResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void unauthenticatedDashboardApiRequestIsForbidden() {
        ResponseEntity<String> response =
                restTemplate.getForEntity("/api/dashboards", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(response.getBody()).contains("\"status\":403")
                .contains("\"error\":\"Forbidden\"")
                .contains("\"message\":\"Access denied\"")
                .contains("\"path\":\"/api/dashboards\"");
    }

    @Test
    void h2ConsoleLoginPageIsAccessibleWithoutAuthentication() {
        ResponseEntity<String> response =
                restTemplate.getForEntity("/h2-console/", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void csrfTokenCanBeUsedForRegistrationOverHttp() throws Exception {
        ResponseEntity<CsrfResponse> csrfResponse =
                restTemplate.getForEntity("/api/auth/csrf", CsrfResponse.class);
        assertThat(csrfResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(csrfResponse.getBody()).isNotNull();

        List<String> cookies = csrfResponse.getHeaders().get(HttpHeaders.SET_COOKIE);
        assertThat(cookies).isNotNull().isNotEmpty();
        String cookieHeader = cookies.stream()
                .map(cookie -> cookie.split(";", 2)[0])
                .collect(Collectors.joining("; "));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.COOKIE, cookieHeader);
        headers.set(csrfResponse.getBody().headerName(), csrfResponse.getBody().token());
        String body = objectMapper.writeValueAsString(new RegistrationPayload(
                "HTTP Tester", "http-" + UUID.randomUUID() + "@example.com", "strong-password"));

        ResponseEntity<String> registrationResponse = restTemplate.postForEntity(
                "/api/auth/register", new HttpEntity<>(body, headers), String.class);

        assertThat(registrationResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    private record RegistrationPayload(String name, String email, String password) {
    }
}
