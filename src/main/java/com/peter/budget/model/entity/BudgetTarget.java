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
public class BudgetTarget {
    private Long id;
    private Long userId;
    private String monthKey;
    private Long categoryId;
    private BigDecimal targetAmount;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;
}
