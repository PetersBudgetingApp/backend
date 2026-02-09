package com.peter.budget.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
public class TransactionCreateRequest {
    @NotNull(message = "Account is required")
    private Long accountId;

    @NotNull(message = "Posted date is required")
    private LocalDate postedDate;

    private LocalDate transactedDate;

    @NotNull(message = "Amount is required")
    private BigDecimal amount;

    @NotBlank(message = "Description is required")
    @Size(max = 500, message = "Description must be at most 500 characters")
    private String description;

    @Size(max = 255, message = "Payee must be at most 255 characters")
    private String payee;

    private String memo;
    private Long categoryId;
    private boolean pending;
    private Boolean excludeFromTotals;
    private String notes;
}
