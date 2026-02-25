package com.peter.budget.model.dto;

import com.peter.budget.model.enums.AccountNetWorthCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class AccountCreateRequest {
    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must be at most 255 characters")
    private String name;

    @Size(max = 255, message = "Institution name must be at most 255 characters")
    private String institutionName;

    @NotNull(message = "Net worth category is required")
    private AccountNetWorthCategory netWorthCategory;

    @NotNull(message = "Current balance is required")
    private BigDecimal currentBalance;

    @Pattern(regexp = "^[A-Za-z]{3}$", message = "Currency code must be 3 characters")
    private String currency;
}
