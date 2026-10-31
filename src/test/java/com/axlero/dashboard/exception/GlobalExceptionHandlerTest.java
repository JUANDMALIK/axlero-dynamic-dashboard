package com.axlero.dashboard.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindException;
import org.springframework.validation.BeanPropertyBindingResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void authenticationFailuresReturnUnauthorizedWithoutExposingExceptionMessage() {
        var response = handler.handleAuthentication(
                new BadCredentialsException("sensitive authentication detail"));

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertFalse(response.getBody().toString().contains("sensitive"));
    }

    @Test
    void validationFailuresReturnBadRequest() {
        var bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        var response = handler.handleInvalidRequest(
                new BindException(bindingResult));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void malformedRequestsReturnBadRequest() {
        var exception = new HttpMessageNotReadableException(
                "malformed request containing a secret",
                new MockHttpInputMessage(new byte[0]));

        var response = handler.handleInvalidRequest(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().toString().contains("secret"));
    }

    @Test
    void unexpectedFailuresReturnGenericInternalServerError() {
        var response = handler.handleGeneralException(
                new IllegalStateException("sensitive internal detail"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertFalse(response.getBody().toString().contains("sensitive"));
    }
}
