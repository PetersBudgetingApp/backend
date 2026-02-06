package com.peter.budget.model.entity;

import com.peter.budget.model.enums.SyncStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimpleFinConnection {
    private Long id;
    private Long userId;
    private String accessUrlEncrypted;
    private String institutionName;
    private Instant lastSyncAt;
    private SyncStatus syncStatus;
    private String errorMessage;
    private int requestsToday;
    private Instant requestsResetAt;
    private Instant createdAt;
    private Instant updatedAt;
}
