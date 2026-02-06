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
public class SpendingByCategoryDto {
    private BigDecimal totalSpending;
    private List<CategorySpending> categories;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategorySpending {
        private Long categoryId;
        private String categoryName;
        private String categoryColor;
        private BigDecimal amount;
        private BigDecimal percentage;
        private int transactionCount;
    }
}
