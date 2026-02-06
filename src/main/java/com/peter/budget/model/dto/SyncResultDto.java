package com.peter.budget.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncResultDto {
    private boolean success;
    private String message;
    private int accountsSynced;
    private int transactionsAdded;
    private int transactionsUpdated;
    private int transfersDetected;
    private Instant syncedAt;
}
