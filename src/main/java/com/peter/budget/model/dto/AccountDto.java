package com.peter.budget.model.dto;

import com.peter.budget.model.enums.AccountType;
import com.peter.budget.model.enums.AccountNetWorthCategory;
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
public class AccountDto {
    private Long id;
    private String name;
    private String institutionName;
    private AccountType accountType;
    private AccountNetWorthCategory netWorthCategory;
    private String currency;
    private BigDecimal currentBalance;
    private BigDecimal availableBalance;
    private Instant balanceUpdatedAt;
    private boolean active;
}
