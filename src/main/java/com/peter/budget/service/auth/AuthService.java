package com.peter.budget.service.auth;

import com.peter.budget.exception.ApiException;
import com.peter.budget.model.dto.AuthRequest;
import com.peter.budget.model.dto.AuthResponse;
import com.peter.budget.model.entity.RefreshToken;
import com.peter.budget.model.entity.User;
import com.peter.budget.repository.RefreshTokenRepository;
import com.peter.budget.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(AuthRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw ApiException.conflict("Email already registered");
        }

        User user = User.builder()
                .email(request.getEmail().toLowerCase().trim())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .build();

        user = userRepository.save(user);

        return generateAuthResponse(user);
    }

    @Transactional
    public AuthResponse login(AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail().toLowerCase().trim(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseThrow(() -> ApiException.unauthorized("Invalid credentials"));

        return generateAuthResponse(user);
    }

    @Transactional
    public AuthResponse refresh(String refreshToken) {
        String tokenHash = jwtService.hashRefreshToken(refreshToken);

        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> ApiException.unauthorized("Invalid refresh token"));

        if (storedToken.getExpiresAt().isBefore(Instant.now())) {
            throw ApiException.unauthorized("Refresh token expired");
        }

        refreshTokenRepository.revokeByTokenHash(tokenHash);

        User user = userRepository.findById(storedToken.getUserId())
                .orElseThrow(() -> ApiException.unauthorized("User not found"));

        return generateAuthResponse(user);
    }

    @Transactional
    public void logout(Long userId, String refreshToken) {
        if (refreshToken != null) {
            String tokenHash = jwtService.hashRefreshToken(refreshToken);
            refreshTokenRepository.revokeByTokenHash(tokenHash);
        } else {
            refreshTokenRepository.revokeByUserId(userId);
        }
    }

    public User getCurrentUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("User not found"));
    }

    private AuthResponse generateAuthResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtService.generateRefreshToken();

        RefreshToken storedToken = RefreshToken.builder()
                .userId(user.getId())
                .tokenHash(jwtService.hashRefreshToken(refreshToken))
                .expiresAt(jwtService.getRefreshTokenExpiration())
                .revoked(false)
                .build();

        refreshTokenRepository.save(storedToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpirationMs() / 1000)
                .user(AuthResponse.UserDto.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .build())
                .build();
    }
}
