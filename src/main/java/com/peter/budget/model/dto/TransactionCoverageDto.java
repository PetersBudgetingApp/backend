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
public class TransactionCoverageDto {
    private long totalTransactions;
    private Instant oldestPostedAt;
    private Instant newestPostedAt;
}

