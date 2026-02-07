package com.peter.budget.service.auth;

import com.peter.budget.exception.ApiException;
import com.peter.budget.model.dto.AuthRequest;
import com.peter.budget.model.dto.AuthResponse;
import com.peter.budget.model.entity.RefreshToken;
import com.peter.budget.model.entity.User;
import com.peter.budget.repository.RefreshTokenRepository;
import com.peter.budget.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final long USER_ID = 1L;
    private static final String EMAIL = "test@example.com";
    private static final String PASSWORD = "password123";
    private static final String ENCODED_PASSWORD = "$2a$10$encodedHash";
    private static final String ACCESS_TOKEN = "jwt-access-token";
    private static final String REFRESH_TOKEN = "random-refresh-token";
    private static final String REFRESH_TOKEN_HASH = "hashed-refresh-token";

    @Mock
    private UserRepository userRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    @Captor
    private ArgumentCaptor<User> userCaptor;
    @Captor
    private ArgumentCaptor<RefreshToken> refreshTokenCaptor;

    @BeforeEach
    void setUp() {
        lenient().when(jwtService.generateAccessToken(USER_ID, EMAIL)).thenReturn(ACCESS_TOKEN);
        lenient().when(jwtService.generateRefreshToken()).thenReturn(REFRESH_TOKEN);
        lenient().when(jwtService.hashRefreshToken(REFRESH_TOKEN)).thenReturn(REFRESH_TOKEN_HASH);
        lenient().when(jwtService.getRefreshTokenExpiration()).thenReturn(Instant.now().plusSeconds(604800));
        lenient().when(jwtService.getAccessTokenExpirationMs()).thenReturn(900000L);
        lenient().when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    // --- register tests ---

    @Test
    void registerCreatesUserAndReturnsTokens() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(passwordEncoder.encode(PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(USER_ID);
            return user;
        });

        AuthResponse response = authService.register(AuthRequest.builder()
                .email(EMAIL)
                .password(PASSWORD)
                .build());

        assertNotNull(response);
        assertEquals(ACCESS_TOKEN, response.getAccessToken());
        assertEquals(REFRESH_TOKEN, response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(900, response.getExpiresIn());
        assertNotNull(response.getUser());
        assertEquals(USER_ID, response.getUser().getId());
        assertEquals(EMAIL, response.getUser().getEmail());

        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals(EMAIL, savedUser.getEmail());
        assertEquals(ENCODED_PASSWORD, savedUser.getPasswordHash());

        verify(refreshTokenRepository).save(refreshTokenCaptor.capture());
        RefreshToken savedToken = refreshTokenCaptor.getValue();
        assertEquals(USER_ID, savedToken.getUserId());
        assertEquals(REFRESH_TOKEN_HASH, savedToken.getTokenHash());
    }

    @Test
    void registerNormalizesEmail() {
        when(userRepository.existsByEmail("  TEST@Example.COM  ")).thenReturn(false);
        when(passwordEncoder.encode(PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(USER_ID);
            return user;
        });

        authService.register(AuthRequest.builder()
                .email("  TEST@Example.COM  ")
                .password(PASSWORD)
                .build());

        verify(userRepository).save(userCaptor.capture());
        assertEquals("test@example.com", userCaptor.getValue().getEmail());
    }

    @Test
    void registerThrowsConflictWhenEmailAlreadyExists() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

        ApiException exception = assertThrows(
                ApiException.class,
                () -> authService.register(AuthRequest.builder()
                        .email(EMAIL)
                        .password(PASSWORD)
                        .build())
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertEquals("Email already registered", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    // --- login tests ---

    @Test
    void loginReturnsTokensOnValidCredentials() {
        User user = User.builder()
                .id(USER_ID)
                .email(EMAIL)
                .passwordHash(ENCODED_PASSWORD)
                .build();

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        AuthResponse response = authService.login(AuthRequest.builder()
                .email(EMAIL)
                .password(PASSWORD)
                .build());

        assertNotNull(response);
        assertEquals(ACCESS_TOKEN, response.getAccessToken());
        assertEquals(REFRESH_TOKEN, response.getRefreshToken());
        assertEquals(USER_ID, response.getUser().getId());

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void loginNormalizesEmail() {
        User user = User.builder()
                .id(USER_ID)
                .email(EMAIL)
                .passwordHash(ENCODED_PASSWORD)
                .build();

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        authService.login(AuthRequest.builder()
                .email("  TEST@Example.COM  ")
                .password(PASSWORD)
                .build());

        verify(userRepository).findByEmail(EMAIL);
    }

    @Test
    void loginThrowsBadCredentialsOnInvalidPassword() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(
                BadCredentialsException.class,
                () -> authService.login(AuthRequest.builder()
                        .email(EMAIL)
                        .password("wrong-password")
                        .build())
        );
    }

    @Test
    void loginThrowsUnauthorizedWhenUserNotFoundAfterAuth() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        ApiException exception = assertThrows(
                ApiException.class,
                () -> authService.login(AuthRequest.builder()
                        .email(EMAIL)
                        .password(PASSWORD)
                        .build())
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
    }

    // --- refresh tests ---

    @Test
    void refreshRevokesOldTokenAndIssuesNewPair() {
        RefreshToken storedToken = RefreshToken.builder()
                .id(10L)
                .userId(USER_ID)
                .tokenHash(REFRESH_TOKEN_HASH)
                .expiresAt(Instant.now().plusSeconds(3600))
                .revoked(false)
                .build();

        User user = User.builder()
                .id(USER_ID)
                .email(EMAIL)
                .build();

        when(jwtService.hashRefreshToken("old-refresh-token")).thenReturn("old-hash");
        when(refreshTokenRepository.findByTokenHash("old-hash")).thenReturn(Optional.of(storedToken));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        AuthResponse response = authService.refresh("old-refresh-token");

        assertNotNull(response);
        assertEquals(ACCESS_TOKEN, response.getAccessToken());
        verify(refreshTokenRepository).revokeByTokenHash("old-hash");
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void refreshThrowsUnauthorizedWhenTokenNotFound() {
        when(jwtService.hashRefreshToken("bad-token")).thenReturn("bad-hash");
        when(refreshTokenRepository.findByTokenHash("bad-hash")).thenReturn(Optional.empty());

        ApiException exception = assertThrows(
                ApiException.class,
                () -> authService.refresh("bad-token")
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        assertEquals("Invalid refresh token", exception.getMessage());
    }

    @Test
    void refreshThrowsUnauthorizedWhenTokenExpired() {
        RefreshToken expiredToken = RefreshToken.builder()
                .id(10L)
                .userId(USER_ID)
                .tokenHash("expired-hash")
                .expiresAt(Instant.now().minusSeconds(3600))
                .revoked(false)
                .build();

        when(jwtService.hashRefreshToken("expired-token")).thenReturn("expired-hash");
        when(refreshTokenRepository.findByTokenHash("expired-hash")).thenReturn(Optional.of(expiredToken));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> authService.refresh("expired-token")
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        assertEquals("Refresh token expired", exception.getMessage());
    }

    @Test
    void refreshThrowsUnauthorizedWhenUserNotFound() {
        RefreshToken storedToken = RefreshToken.builder()
                .id(10L)
                .userId(999L)
                .tokenHash("valid-hash")
                .expiresAt(Instant.now().plusSeconds(3600))
                .revoked(false)
                .build();

        when(jwtService.hashRefreshToken("valid-token")).thenReturn("valid-hash");
        when(refreshTokenRepository.findByTokenHash("valid-hash")).thenReturn(Optional.of(storedToken));
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        ApiException exception = assertThrows(
                ApiException.class,
                () -> authService.refresh("valid-token")
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
    }

    // --- logout tests ---

    @Test
    void logoutRevokesSpecificTokenWhenProvided() {
        when(jwtService.hashRefreshToken(REFRESH_TOKEN)).thenReturn(REFRESH_TOKEN_HASH);

        authService.logout(USER_ID, REFRESH_TOKEN);

        verify(refreshTokenRepository).revokeByTokenHash(REFRESH_TOKEN_HASH);
        verify(refreshTokenRepository, never()).revokeByUserId(USER_ID);
    }

    @Test
    void logoutRevokesAllTokensWhenNoTokenProvided() {
        authService.logout(USER_ID, null);

        verify(refreshTokenRepository).revokeByUserId(USER_ID);
        verify(refreshTokenRepository, never()).revokeByTokenHash(anyString());
    }

    // --- getCurrentUser tests ---

    @Test
    void getCurrentUserReturnsUser() {
        User user = User.builder()
                .id(USER_ID)
                .email(EMAIL)
                .build();

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        User result = authService.getCurrentUser(USER_ID);

        assertEquals(USER_ID, result.getId());
        assertEquals(EMAIL, result.getEmail());
    }

    @Test
    void getCurrentUserThrowsNotFoundWhenUserDoesNotExist() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        ApiException exception = assertThrows(
                ApiException.class,
                () -> authService.getCurrentUser(999L)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }
}
