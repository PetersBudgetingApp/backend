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
public class TransferPairDto {
    private Long fromTransactionId;
    private String fromAccountName;
    private Long toTransactionId;
    private String toAccountName;
    private BigDecimal amount;
    private Instant date;
    private String description;
    private boolean autoDetected;
}
