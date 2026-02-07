package com.peter.budget.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetMonthDto {
    private String month;
    private String currency;
    private List<BudgetTargetDto> targets;
}
