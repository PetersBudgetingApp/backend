package com.peter.budget.model.dto;

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
public class ConnectionDto {
    private Long id;
    private String institutionName;
    private Instant lastSyncAt;
    private SyncStatus syncStatus;
    private String errorMessage;
    private int accountCount;
}
