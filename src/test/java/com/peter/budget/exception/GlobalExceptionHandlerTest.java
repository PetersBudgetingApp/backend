package com.peter.budget.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleApiExceptionReturnsCorrectStatusAndPayload() {
        ApiException ex = ApiException.notFound("Resource not found");

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleApiException(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, response.getBody().status());
        assertEquals("Resource not found", response.getBody().message());
        assertNotNull(response.getBody().timestamp());
    }

    @Test
    void handleApiExceptionReturns400ForBadRequest() {
        ApiException ex = ApiException.badRequest("Invalid input");

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleApiException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, response.getBody().status());
    }

    @Test
    void handleApiExceptionReturns401ForUnauthorized() {
        ApiException ex = ApiException.unauthorized("Not authenticated");

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleApiException(ex);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(401, response.getBody().status());
    }

    @Test
    void handleApiExceptionReturns409ForConflict() {
        ApiException ex = ApiException.conflict("Already exists");

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleApiException(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(409, response.getBody().status());
    }

    @Test
    void handleApiExceptionReturns429ForTooManyRequests() {
        ApiException ex = ApiException.tooManyRequests("Rate limit exceeded");

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleApiException(ex);

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
        assertEquals(429, response.getBody().status());
    }

    @Test
    void handleBadCredentialsReturns401() {
        BadCredentialsException ex = new BadCredentialsException("Bad credentials");

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleBadCredentials(ex);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(401, response.getBody().status());
        assertEquals("Invalid email or password", response.getBody().message());
        assertNotNull(response.getBody().timestamp());
    }

    @Test
    void handleValidationExceptionsReturns400WithFieldErrors() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "email", "Email is required"));
        bindingResult.addError(new FieldError("request", "password", "Password must be at least 8 characters"));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<Map<String, Object>> response = handler.handleValidationExceptions(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, response.getBody().get("status"));
        assertEquals("Validation failed", response.getBody().get("message"));
        assertNotNull(response.getBody().get("timestamp"));

        @SuppressWarnings("unchecked")
        Map<String, String> errors = (Map<String, String>) response.getBody().get("errors");
        assertEquals("Email is required", errors.get("email"));
        assertEquals("Password must be at least 8 characters", errors.get("password"));
    }

    @Test
    void handleGenericExceptionReturns500() {
        RuntimeException ex = new RuntimeException("Something went wrong");

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleGenericException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(500, response.getBody().status());
        assertEquals("An unexpected error occurred", response.getBody().message());
        assertNotNull(response.getBody().timestamp());
    }
}
