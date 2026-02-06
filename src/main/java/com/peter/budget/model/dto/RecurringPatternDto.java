package com.peter.budget.model.dto;

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
public class RecurringPatternDto {
    private Long id;
    private String name;
    private String merchantPattern;
    private BigDecimal expectedAmount;
    private Frequency frequency;
    private Integer dayOfMonth;
    private LocalDate nextExpectedDate;
    private CategoryDto category;
    private boolean bill;
    private boolean active;
    private Instant lastOccurrenceAt;
}
