package com.peter.budget.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetTargetDto {
    private Long categoryId;
    private BigDecimal targetAmount;
    private String notes;
}
