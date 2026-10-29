package com.axlero.dashboard;

import com.axlero.dashboard.api.ApiErrorResponse;
import com.axlero.dashboard.api.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

    @Test
    void mapsDatabaseConstraintFailuresToSafeConflictResponses() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/register");
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleDataIntegrityViolation(
                new DataIntegrityViolationException("database detail that should not be exposed"),
                request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(409);
        assertThat(response.getBody().error()).isEqualTo("Conflict");
        assertThat(response.getBody().message()).isEqualTo("A database constraint was violated");
        assertThat(response.getBody().path()).isEqualTo("/api/auth/register");
        assertThat(response.getBody().validationErrors()).isEmpty();
    }

    @Test
    void standardizesStatusErrors() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/dashboards/1");
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleResponseStatus(
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Dashboard not found"),
                request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Dashboard not found");
        assertThat(response.getBody().path()).isEqualTo("/api/dashboards/1");
    }
}
