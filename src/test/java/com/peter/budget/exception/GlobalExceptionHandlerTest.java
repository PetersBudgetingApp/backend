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
import java.util.Objects;

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

        GlobalExceptionHandler.ErrorResponse body = Objects.requireNonNull(response.getBody());

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, body.status());
        assertEquals("Resource not found", body.message());
        assertNotNull(body.timestamp());
    }

    @Test
    void handleApiExceptionReturns400ForBadRequest() {
        ApiException ex = ApiException.badRequest("Invalid input");

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleApiException(ex);

        GlobalExceptionHandler.ErrorResponse body = Objects.requireNonNull(response.getBody());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, body.status());
    }

    @Test
    void handleApiExceptionReturns401ForUnauthorized() {
        ApiException ex = ApiException.unauthorized("Not authenticated");

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleApiException(ex);

        GlobalExceptionHandler.ErrorResponse body = Objects.requireNonNull(response.getBody());

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(401, body.status());
    }

    @Test
    void handleApiExceptionReturns409ForConflict() {
        ApiException ex = ApiException.conflict("Already exists");

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleApiException(ex);

        GlobalExceptionHandler.ErrorResponse body = Objects.requireNonNull(response.getBody());

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(409, body.status());
    }

    @Test
    void handleApiExceptionReturns429ForTooManyRequests() {
        ApiException ex = ApiException.tooManyRequests("Rate limit exceeded");

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleApiException(ex);

        GlobalExceptionHandler.ErrorResponse body = Objects.requireNonNull(response.getBody());

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
        assertEquals(429, body.status());
    }

    @Test
    void handleBadCredentialsReturns401() {
        BadCredentialsException ex = new BadCredentialsException("Bad credentials");

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleBadCredentials(ex);

        GlobalExceptionHandler.ErrorResponse body = Objects.requireNonNull(response.getBody());

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(401, body.status());
        assertEquals("Invalid email or password", body.message());
        assertNotNull(body.timestamp());
    }

    @Test
    void handleValidationExceptionsReturns400WithFieldErrors() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "email", "Email is required"));
        bindingResult.addError(new FieldError("request", "password", "Password must be at least 8 characters"));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<Map<String, Object>> response = handler.handleValidationExceptions(ex);

        Map<String, Object> body = Objects.requireNonNull(response.getBody());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, body.get("status"));
        assertEquals("Validation failed", body.get("message"));
        assertNotNull(body.get("timestamp"));

        @SuppressWarnings("unchecked")
        Map<String, String> errors = (Map<String, String>) body.get("errors");
        assertEquals("Email is required", errors.get("email"));
        assertEquals("Password must be at least 8 characters", errors.get("password"));
    }

    @Test
    void handleGenericExceptionReturns500() {
        RuntimeException ex = new RuntimeException("Something went wrong");

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleGenericException(ex);

        GlobalExceptionHandler.ErrorResponse body = Objects.requireNonNull(response.getBody());

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(500, body.status());
        assertEquals("An unexpected error occurred", body.message());
        assertNotNull(body.timestamp());
    }
}