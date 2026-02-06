package com.peter.budget.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {
    private Long id;
    private Long userId;
    private String tokenHash;
    private Instant expiresAt;
    private boolean revoked;
    private Instant createdAt;
}
