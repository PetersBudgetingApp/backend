package com.peter.budget.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendDto {
    private List<MonthlyTrend> trends;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyTrend {
        private YearMonth month;
        private BigDecimal income;
        private BigDecimal expenses;
        private BigDecimal transfers;
        private BigDecimal netCashFlow;
    }
}
