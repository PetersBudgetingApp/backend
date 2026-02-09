package com.peter.budget.model.dto;

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
public class TransactionDto {
    private Long id;
    private Long accountId;
    private String accountName;
    private Instant postedAt;
    private Instant transactedAt;
    private BigDecimal amount;
    private boolean pending;
    private String description;
    private String payee;
    private String memo;
    private CategoryDto category;
    private boolean manuallyCategorized;
    private boolean internalTransfer;
    private boolean excludeFromTotals;
    private Long transferPairId;
    private String transferPairAccountName;
    private boolean recurring;
    private String notes;
    private boolean manualEntry;
}
