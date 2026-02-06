package com.peter.budget.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountSummaryDto {
    private BigDecimal totalAssets;
    private BigDecimal totalLiabilities;
    private BigDecimal netWorth;
    private List<AccountDto> accounts;
}
