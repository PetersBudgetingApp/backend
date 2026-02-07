package com.peter.budget.service.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private static final String SECRET = "this-is-a-very-long-secret-key-for-testing-jwt-tokens-minimum-256-bits";
    private static final long ACCESS_TOKEN_EXPIRATION_MS = 900000;
    private static final long REFRESH_TOKEN_EXPIRATION_MS = 604800000;

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, ACCESS_TOKEN_EXPIRATION_MS, REFRESH_TOKEN_EXPIRATION_MS);
    }

    @Test
    void generateAccessTokenProducesValidToken() {
        String token = jwtService.generateAccessToken(1L, "test@example.com");

        assertNotNull(token);
        assertTrue(jwtService.isTokenValid(token));
    }

    @Test
    void extractUserIdFromToken() {
        String token = jwtService.generateAccessToken(42L, "user@example.com");

        assertEquals(42L, jwtService.extractUserId(token));
    }

    @Test
    void extractEmailFromToken() {
        String token = jwtService.generateAccessToken(1L, "user@example.com");

        assertEquals("user@example.com", jwtService.extractEmail(token));
    }

    @Test
    void isTokenValidReturnsFalseForGarbageToken() {
        assertFalse(jwtService.isTokenValid("not-a-valid-token"));
    }

    @Test
    void isTokenValidReturnsFalseForTokenSignedWithDifferentKey() {
        JwtService otherService = new JwtService(
                "a-completely-different-secret-key-that-is-long-enough-for-hmac-sha256",
                ACCESS_TOKEN_EXPIRATION_MS, REFRESH_TOKEN_EXPIRATION_MS);
        String token = otherService.generateAccessToken(1L, "test@example.com");

        assertFalse(jwtService.isTokenValid(token));
    }

    @Test
    void isTokenValidReturnsFalseForExpiredToken() {
        JwtService shortLivedService = new JwtService(SECRET, 0, REFRESH_TOKEN_EXPIRATION_MS);
        String token = shortLivedService.generateAccessToken(1L, "test@example.com");

        assertFalse(jwtService.isTokenValid(token));
    }

    @Test
    void generateRefreshTokenProducesUniqueValues() {
        String token1 = jwtService.generateRefreshToken();
        String token2 = jwtService.generateRefreshToken();

        assertNotNull(token1);
        assertNotNull(token2);
        assertNotEquals(token1, token2);
    }

    @Test
    void hashRefreshTokenProducesDeterministicHash() {
        String hash1 = jwtService.hashRefreshToken("some-token");
        String hash2 = jwtService.hashRefreshToken("some-token");

        assertEquals(hash1, hash2);
    }

    @Test
    void hashRefreshTokenProducesDifferentHashForDifferentInput() {
        String hash1 = jwtService.hashRefreshToken("token-a");
        String hash2 = jwtService.hashRefreshToken("token-b");

        assertNotEquals(hash1, hash2);
    }

    @Test
    void getAccessTokenExpirationMsReturnsConfiguredValue() {
        assertEquals(ACCESS_TOKEN_EXPIRATION_MS, jwtService.getAccessTokenExpirationMs());
    }

    @Test
    void getRefreshTokenExpirationReturnsFutureInstant() {
        Instant expiration = jwtService.getRefreshTokenExpiration();

        assertTrue(expiration.isAfter(Instant.now()));
    }
}
