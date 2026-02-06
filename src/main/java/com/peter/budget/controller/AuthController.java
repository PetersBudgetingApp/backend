package com.peter.budget.controller;

import com.peter.budget.config.JwtAuthFilter;
import com.peter.budget.model.dto.AuthRequest;
import com.peter.budget.model.dto.AuthResponse;
import com.peter.budget.model.dto.RefreshTokenRequest;
import com.peter.budget.model.entity.User;
import com.peter.budget.service.auth.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody AuthRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refresh(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal JwtAuthFilter.UserPrincipal principal,
            @RequestBody(required = false) RefreshTokenRequest request) {
        String refreshToken = request != null ? request.getRefreshToken() : null;
        authService.logout(principal.userId(), refreshToken);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse.UserDto> me(
            @AuthenticationPrincipal JwtAuthFilter.UserPrincipal principal) {
        User user = authService.getCurrentUser(principal.userId());
        return ResponseEntity.ok(AuthResponse.UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .build());
    }
}
