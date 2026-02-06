package com.peter.budget.model.entity;

import com.peter.budget.model.enums.Frequency;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecurringPattern {
    private Long id;
    private Long userId;
    private String name;
    private String merchantPattern;
    private BigDecimal expectedAmount;
    private BigDecimal amountVariance;
    private Frequency frequency;
    private Integer dayOfWeek;
    private Integer dayOfMonth;
    private LocalDate nextExpectedDate;
    private Long categoryId;
    private boolean bill;
    private boolean active;
    private Instant lastOccurrenceAt;
    private Instant createdAt;
    private Instant updatedAt;
}
