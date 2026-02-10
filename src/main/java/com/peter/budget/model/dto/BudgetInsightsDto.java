package com.peter.budget.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetInsightsDto {
    private String month;
    private LocalDate asOfDate;
    private int historyMonths;
    private BigDecimal totalCurrentBudget;
    private BigDecimal totalRecommendedBudget;
    private List<CategoryInsight> categories;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryInsight {
        private Long categoryId;
        private String categoryName;
        private String categoryColor;
        private BigDecimal currentBudget;
        private BigDecimal currentMonthSpend;
        private BigDecimal averageMonthlySpend;
        private BigDecimal recommendedBudget;
        private BigDecimal budgetDelta;
        private BigDecimal budgetDeltaPct;
        private BigDecimal monthToDateSpend;
        private BigDecimal averageMonthToDateSpend;
        private BigDecimal monthToDateDelta;
        private BigDecimal monthToDateDeltaPct;
    }
}
