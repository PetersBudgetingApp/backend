package com.peter.budget.service;

import com.peter.budget.exception.ApiException;
import com.peter.budget.model.dto.BudgetInsightsDto;
import com.peter.budget.model.dto.CashFlowDto;
import com.peter.budget.model.dto.SpendingByCategoryDto;
import com.peter.budget.model.dto.TrendDto;
import com.peter.budget.model.entity.BudgetTarget;
import com.peter.budget.model.entity.Category;
import com.peter.budget.model.enums.CategoryType;
import com.peter.budget.repository.BudgetTargetRepository;
import com.peter.budget.repository.TransactionAnalyticsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    private static final long USER_ID = 7L;

    @Mock
    private TransactionAnalyticsRepository transactionAnalyticsRepository;
    @Mock
    private BudgetTargetRepository budgetTargetRepository;
    @Mock
    private CategoryViewService categoryViewService;

    @InjectMocks
    private AnalyticsService analyticsService;

    // --- getSpendingByCategory tests ---

    @Test
    void getSpendingByCategoryReturnsGroupedSpending() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 1, 31);

        Category groceries = Category.builder()
                .id(10L).name("Groceries").color("#22C55E").categoryType(CategoryType.EXPENSE).build();

        when(transactionAnalyticsRepository.sumByCategory(USER_ID, start, end))
                .thenReturn(List.of(
                        new TransactionAnalyticsRepository.CategorySpendingProjection(10L, new BigDecimal("300.00"), 15),
                        new TransactionAnalyticsRepository.CategorySpendingProjection(null, new BigDecimal("100.00"), 5)
                ));
        when(categoryViewService.getEffectiveCategoryMapForUser(USER_ID))
                .thenReturn(Map.of(10L, groceries));

        SpendingByCategoryDto result = analyticsService.getSpendingByCategory(USER_ID, start, end);

        assertNotNull(result);
        assertEquals(new BigDecimal("400.00"), result.getTotalSpending());
        assertEquals(2, result.getCategories().size());
        assertEquals("Groceries", result.getCategories().get(0).getCategoryName());
        assertEquals("#22C55E", result.getCategories().get(0).getCategoryColor());
        assertEquals(15, result.getCategories().get(0).getTransactionCount());
        assertEquals("Uncategorized", result.getCategories().get(1).getCategoryName());
    }

    @Test
    void getSpendingByCategoryDefaultsToCurrentMonth() {
        when(transactionAnalyticsRepository.sumByCategory(eq(USER_ID), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());
        when(categoryViewService.getEffectiveCategoryMapForUser(USER_ID))
                .thenReturn(Map.of());

        SpendingByCategoryDto result = analyticsService.getSpendingByCategory(USER_ID, null, null);

        assertNotNull(result);
        assertEquals(BigDecimal.ZERO, result.getTotalSpending());
        assertTrue(result.getCategories().isEmpty());
        verify(transactionAnalyticsRepository).sumByCategory(eq(USER_ID),
                eq(LocalDate.now().withDayOfMonth(1)), eq(LocalDate.now()));
    }

    @Test
    void getSpendingByCategoryCalculatesPercentages() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 1, 31);

        when(transactionAnalyticsRepository.sumByCategory(USER_ID, start, end))
                .thenReturn(List.of(
                        new TransactionAnalyticsRepository.CategorySpendingProjection(1L, new BigDecimal("750.00"), 10),
                        new TransactionAnalyticsRepository.CategorySpendingProjection(2L, new BigDecimal("250.00"), 5)
                ));
        when(categoryViewService.getEffectiveCategoryMapForUser(USER_ID))
                .thenReturn(Map.of(
                        1L, Category.builder().id(1L).name("A").color("#000").categoryType(CategoryType.EXPENSE).build(),
                        2L, Category.builder().id(2L).name("B").color("#FFF").categoryType(CategoryType.EXPENSE).build()
                ));

        SpendingByCategoryDto result = analyticsService.getSpendingByCategory(USER_ID, start, end);

        assertEquals(new BigDecimal("75.00"), result.getCategories().get(0).getPercentage());
        assertEquals(new BigDecimal("25.00"), result.getCategories().get(1).getPercentage());
    }

    @Test
    void getSpendingByCategoryHandlesZeroTotal() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 1, 31);

        when(transactionAnalyticsRepository.sumByCategory(USER_ID, start, end))
                .thenReturn(List.of());
        when(categoryViewService.getEffectiveCategoryMapForUser(USER_ID))
                .thenReturn(Map.of());

        SpendingByCategoryDto result = analyticsService.getSpendingByCategory(USER_ID, start, end);

        assertEquals(BigDecimal.ZERO, result.getTotalSpending());
        assertTrue(result.getCategories().isEmpty());
    }

    // --- getCashFlow tests ---

    @Test
    void getCashFlowReturnsIncomeExpensesAndSavingsRate() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 1, 31);

        when(transactionAnalyticsRepository.sumByUserIdAndDateRangeAndType(USER_ID, start, end, true, true))
                .thenReturn(new BigDecimal("5000.00"));
        when(transactionAnalyticsRepository.sumByUserIdAndDateRangeAndType(USER_ID, start, end, false, true))
                .thenReturn(new BigDecimal("-3000.00"));
        when(transactionAnalyticsRepository.sumByUserIdAndDateRangeAndType(USER_ID, start, end, false, false))
                .thenReturn(new BigDecimal("-3500.00"));

        CashFlowDto result = analyticsService.getCashFlow(USER_ID, start, end);

        assertNotNull(result);
        assertEquals(start, result.getStartDate());
        assertEquals(end, result.getEndDate());
        assertEquals(new BigDecimal("5000.00"), result.getTotalIncome());
        assertEquals(new BigDecimal("3000.00"), result.getTotalExpenses());
        assertEquals(new BigDecimal("500.00"), result.getTotalTransfers());
        assertEquals(new BigDecimal("2000.00"), result.getNetCashFlow());
        assertEquals(new BigDecimal("40.00"), result.getSavingsRate());
    }

    @Test
    void getCashFlowDefaultsToCurrentMonth() {
        when(transactionAnalyticsRepository.sumByUserIdAndDateRangeAndType(
                eq(USER_ID), any(LocalDate.class), any(LocalDate.class), anyBoolean(), anyBoolean()))
                .thenReturn(BigDecimal.ZERO);

        CashFlowDto result = analyticsService.getCashFlow(USER_ID, null, null);

        assertNotNull(result);
        assertEquals(LocalDate.now().withDayOfMonth(1), result.getStartDate());
        assertEquals(LocalDate.now(), result.getEndDate());
    }

    @Test
    void getCashFlowHandlesZeroIncome() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 1, 31);

        when(transactionAnalyticsRepository.sumByUserIdAndDateRangeAndType(USER_ID, start, end, true, true))
                .thenReturn(BigDecimal.ZERO);
        when(transactionAnalyticsRepository.sumByUserIdAndDateRangeAndType(USER_ID, start, end, false, true))
                .thenReturn(new BigDecimal("-500.00"));
        when(transactionAnalyticsRepository.sumByUserIdAndDateRangeAndType(USER_ID, start, end, false, false))
                .thenReturn(new BigDecimal("-500.00"));

        CashFlowDto result = analyticsService.getCashFlow(USER_ID, start, end);

        assertEquals(BigDecimal.ZERO, result.getSavingsRate());
    }

    // --- getTrends tests ---

    @Test
    void getTrendsReturnsMonthlyTrendData() {
        when(transactionAnalyticsRepository.sumByUserIdAndDateRangeAndType(
                eq(USER_ID), any(LocalDate.class), any(LocalDate.class), anyBoolean(), anyBoolean()))
                .thenReturn(BigDecimal.ZERO);

        TrendDto result = analyticsService.getTrends(USER_ID, 3);

        assertNotNull(result);
        assertEquals(3, result.getTrends().size());

        YearMonth currentMonth = YearMonth.now();
        assertEquals(currentMonth.minusMonths(2), result.getTrends().get(0).getMonth());
        assertEquals(currentMonth.minusMonths(1), result.getTrends().get(1).getMonth());
        assertEquals(currentMonth, result.getTrends().get(2).getMonth());
    }

    @Test
    void getTrendsCalculatesNetCashFlow() {
        YearMonth currentMonth = YearMonth.now();
        LocalDate start = currentMonth.atDay(1);
        LocalDate end = currentMonth.atEndOfMonth();

        when(transactionAnalyticsRepository.sumByUserIdAndDateRangeAndType(USER_ID, start, end, true, true))
                .thenReturn(new BigDecimal("4000.00"));
        when(transactionAnalyticsRepository.sumByUserIdAndDateRangeAndType(USER_ID, start, end, false, true))
                .thenReturn(new BigDecimal("-2500.00"));
        when(transactionAnalyticsRepository.sumByUserIdAndDateRangeAndType(USER_ID, start, end, false, false))
                .thenReturn(new BigDecimal("-2500.00"));

        TrendDto result = analyticsService.getTrends(USER_ID, 1);

        assertEquals(1, result.getTrends().size());
        TrendDto.MonthlyTrend trend = result.getTrends().get(0);
        assertEquals(new BigDecimal("4000.00"), trend.getIncome());
        assertEquals(new BigDecimal("2500.00"), trend.getExpenses());
        assertEquals(new BigDecimal("1500.00"), trend.getNetCashFlow());
    }

    // --- getBudgetInsights tests ---

    @Test
    void getBudgetInsightsReturnsRecommendationsAndMonthToDateDelta() {
        YearMonth targetMonth = YearMonth.now().plusMonths(1);
        YearMonth baselineOne = targetMonth.minusMonths(1);
        YearMonth baselineTwo = targetMonth.minusMonths(2);

        Category groceries = Category.builder()
                .id(10L)
                .name("Groceries")
                .color("#22C55E")
                .categoryType(CategoryType.EXPENSE)
                .build();

        when(categoryViewService.getEffectiveCategoryMapForUser(USER_ID))
                .thenReturn(Map.of(10L, groceries));
        when(budgetTargetRepository.findByUserIdAndMonthKey(USER_ID, targetMonth.toString()))
                .thenReturn(List.of(
                        BudgetTarget.builder().categoryId(10L).targetAmount(new BigDecimal("400.00")).build()
                ));
        when(transactionAnalyticsRepository.sumByCategory(eq(USER_ID), any(LocalDate.class), any(LocalDate.class)))
                .thenAnswer(invocation -> {
                    LocalDate start = invocation.getArgument(1);
                    LocalDate end = invocation.getArgument(2);
                    YearMonth month = YearMonth.from(start);
                    boolean monthToDateCall = end.getDayOfMonth() == 1;

                    if (month.equals(targetMonth)) {
                        return List.of(new TransactionAnalyticsRepository.CategorySpendingProjection(
                                10L, monthToDateCall ? new BigDecimal("20.00") : new BigDecimal("500.00"), 4
                        ));
                    }
                    if (month.equals(baselineOne)) {
                        return List.of(new TransactionAnalyticsRepository.CategorySpendingProjection(
                                10L, monthToDateCall ? new BigDecimal("10.00") : new BigDecimal("300.00"), 3
                        ));
                    }
                    if (month.equals(baselineTwo)) {
                        return List.of(new TransactionAnalyticsRepository.CategorySpendingProjection(
                                10L, monthToDateCall ? new BigDecimal("30.00") : new BigDecimal("100.00"), 2
                        ));
                    }

                    return List.of();
                });

        BudgetInsightsDto result = analyticsService.getBudgetInsights(USER_ID, targetMonth.toString(), 2);

        assertEquals(targetMonth.toString(), result.getMonth());
        assertEquals(1, result.getCategories().size());

        BudgetInsightsDto.CategoryInsight groceriesInsight = result.getCategories().get(0);
        assertEquals(new BigDecimal("400.00"), groceriesInsight.getCurrentBudget());
        assertEquals(new BigDecimal("500.00"), groceriesInsight.getCurrentMonthSpend());
        assertEquals(new BigDecimal("200.00"), groceriesInsight.getAverageMonthlySpend());
        assertEquals(new BigDecimal("233.33"), groceriesInsight.getRecommendedBudget());
        assertEquals(new BigDecimal("-166.67"), groceriesInsight.getBudgetDelta());
        assertEquals(new BigDecimal("-41.67"), groceriesInsight.getBudgetDeltaPct());
        assertEquals(new BigDecimal("20.00"), groceriesInsight.getMonthToDateSpend());
        assertEquals(new BigDecimal("20.00"), groceriesInsight.getAverageMonthToDateSpend());
        assertEquals(new BigDecimal("0.00"), groceriesInsight.getMonthToDateDelta());
        assertEquals(new BigDecimal("0.00"), groceriesInsight.getMonthToDateDeltaPct());
    }

    @Test
    void getBudgetInsightsWeightsRecentMonthsMoreHeavilyForRecommendation() {
        YearMonth targetMonth = YearMonth.now().plusMonths(1);
        YearMonth baselineOne = targetMonth.minusMonths(1);
        YearMonth baselineTwo = targetMonth.minusMonths(2);
        YearMonth baselineThree = targetMonth.minusMonths(3);

        Category travel = Category.builder()
                .id(21L)
                .name("Travel")
                .color("#2563EB")
                .categoryType(CategoryType.EXPENSE)
                .build();

        when(categoryViewService.getEffectiveCategoryMapForUser(USER_ID))
                .thenReturn(Map.of(21L, travel));
        when(budgetTargetRepository.findByUserIdAndMonthKey(USER_ID, targetMonth.toString()))
                .thenReturn(List.of(
                        BudgetTarget.builder().categoryId(21L).targetAmount(new BigDecimal("0.00")).build()
                ));
        when(transactionAnalyticsRepository.sumByCategory(eq(USER_ID), any(LocalDate.class), any(LocalDate.class)))
                .thenAnswer(invocation -> {
                    LocalDate start = invocation.getArgument(1);
                    LocalDate end = invocation.getArgument(2);
                    YearMonth month = YearMonth.from(start);
                    boolean monthToDateCall = end.getDayOfMonth() == 1;

                    if (monthToDateCall || month.equals(targetMonth)) {
                        return List.of(
                                new TransactionAnalyticsRepository.CategorySpendingProjection(21L, BigDecimal.ZERO, 0)
                        );
                    }
                    if (month.equals(baselineOne)) {
                        return List.of(
                                new TransactionAnalyticsRepository.CategorySpendingProjection(21L, new BigDecimal("500.00"), 2)
                        );
                    }
                    if (month.equals(baselineTwo)) {
                        return List.of(
                                new TransactionAnalyticsRepository.CategorySpendingProjection(21L, new BigDecimal("100.00"), 2)
                        );
                    }
                    if (month.equals(baselineThree)) {
                        return List.of(
                                new TransactionAnalyticsRepository.CategorySpendingProjection(21L, new BigDecimal("50.00"), 1)
                        );
                    }
                    return List.of();
                });

        BudgetInsightsDto result = analyticsService.getBudgetInsights(USER_ID, targetMonth.toString(), 3);

        assertEquals(1, result.getCategories().size());
        BudgetInsightsDto.CategoryInsight insight = result.getCategories().get(0);

        assertEquals(new BigDecimal("216.67"), insight.getAverageMonthlySpend());
        assertEquals(new BigDecimal("321.43"), insight.getRecommendedBudget());
    }

    @Test
    void getBudgetInsightsRejectsInvalidHistoryMonths() {
        ApiException exception = assertThrows(
                ApiException.class,
                () -> analyticsService.getBudgetInsights(USER_ID, null, 0)
        );

        assertEquals("historyMonths must be between 1 and 24", exception.getMessage());
    }
}
