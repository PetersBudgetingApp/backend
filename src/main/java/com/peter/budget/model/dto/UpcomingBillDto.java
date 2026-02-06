package com.peter.budget.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpcomingBillDto {
    private Long patternId;
    private String name;
    private BigDecimal expectedAmount;
    private LocalDate dueDate;
    private int daysUntilDue;
    private CategoryDto category;
    private boolean overdue;
}
