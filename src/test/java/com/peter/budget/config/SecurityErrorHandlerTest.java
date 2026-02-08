package com.peter.budget.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SecurityErrorHandlerTest {

    private SecurityErrorHandler handler;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        handler = new SecurityErrorHandler();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    void commenceReturns401WithStandardJsonPayload() throws Exception {
        handler.commence(request, response, new BadCredentialsException("Bad credentials"));

        assertEquals(401, response.getStatus());
        assertEquals("application/json", response.getContentType());

        Map<String, Object> body = objectMapper.readValue(
            response.getContentAsString(),
            new TypeReference<Map<String, Object>>() {}
        );
        assertEquals(401, body.get("status"));
        assertEquals("Authentication required", body.get("message"));
        assertNotNull(body.get("timestamp"));
    }

    @Test
    void handleReturns403WithStandardJsonPayload() throws Exception {
        handler.handle(request, response, new AccessDeniedException("Forbidden"));

        assertEquals(403, response.getStatus());
        assertEquals("application/json", response.getContentType());

        Map<String, Object> body = objectMapper.readValue(
            response.getContentAsString(),
            new TypeReference<Map<String, Object>>() {}
        );
        assertEquals(403, body.get("status"));
        assertEquals("Access denied", body.get("message"));
        assertNotNull(body.get("timestamp"));
    }
}
