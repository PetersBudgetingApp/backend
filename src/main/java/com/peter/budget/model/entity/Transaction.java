package com.peter.budget.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
    private Long id;
    private Long accountId;
    private String externalId;
    private Instant postedAt;
    private Instant transactedAt;
    private BigDecimal amount;
    private boolean pending;
    private String description;
    private String payee;
    private String memo;
    private Long categoryId;
    private boolean manuallyCategorized;
    private Long transferPairId;
    private boolean internalTransfer;
    private boolean excludeFromTotals;
    private boolean recurring;
    private Long recurringPatternId;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;
}
