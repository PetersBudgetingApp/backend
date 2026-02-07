package com.peter.budget.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetTargetUpsertRequest {
    @NotNull(message = "categoryId is required")
    private Long categoryId;

    @NotNull(message = "targetAmount is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "targetAmount must be greater than or equal to 0")
    private BigDecimal targetAmount;

    @Size(max = 1000, message = "notes must be at most 1000 characters")
    private String notes;
}
