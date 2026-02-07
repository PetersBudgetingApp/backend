package com.peter.budget.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategorizationRuleBackfillResultDto {
    private int totalTransactions;
    private int eligibleTransactions;
    private int matchedTransactions;
    private int updatedTransactions;
}
