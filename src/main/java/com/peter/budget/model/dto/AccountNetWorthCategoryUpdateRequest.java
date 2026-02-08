package com.peter.budget.model.dto;

import com.peter.budget.model.enums.AccountNetWorthCategory;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AccountNetWorthCategoryUpdateRequest {
    @NotNull(message = "Net worth category is required")
    private AccountNetWorthCategory netWorthCategory;
}
