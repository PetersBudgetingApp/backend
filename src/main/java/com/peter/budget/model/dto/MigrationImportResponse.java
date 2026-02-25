package com.peter.budget.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MigrationImportResponse {
    private int accountsImported;
    private int categoriesImported;
    private int transactionsImported;
    private int rulesImported;
    private int budgetsImported;
    private int recurringImported;
}
