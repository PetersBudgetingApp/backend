package com.peter.budget.service;

import com.peter.budget.exception.ApiException;
import com.peter.budget.model.dto.BudgetInsightsDto;
import com.peter.budget.model.dto.CashFlowDto;
import com.peter.budget.model.dto.SpendingByCategoryDto;
import com.peter.budget.model.dto.TrendDto;
import com.peter.budget.model.entity.Category;
import com.peter.budget.model.enums.CategoryType;
import com.peter.budget.repository.BudgetTargetRepository;
import com.peter.budget.repository.TransactionAnalyticsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private static final int DEFAULT_HISTORY_MONTHS = 6;
    private static final int MIN_HISTORY_MONTHS = 1;
    private static final int MAX_HISTORY_MONTHS = 24;

    private final TransactionAnalyticsRepository transactionAnalyticsRepository;
    private final BudgetTargetRepository budgetTargetRepository;
    private final CategoryViewService categoryViewService;

    public SpendingByCategoryDto getSpendingByCategory(Long userId, LocalDate startDate, LocalDate endDate) {
        if (startDate == null) {
            startDate = LocalDate.now().withDayOfMonth(1);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        List<TransactionAnalyticsRepository.CategorySpendingProjection> results =
                transactionAnalyticsRepository.sumByCategory(userId, startDate, endDate);

        Map<Long, Category> categoryMap = categoryViewService.getEffectiveCategoryMapForUser(userId);

        BigDecimal totalSpending = results.stream()
                .map(TransactionAnalyticsRepository.CategorySpendingProjection::totalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<SpendingByCategoryDto.CategorySpending> categories = new ArrayList<>();

        for (TransactionAnalyticsRepository.CategorySpendingProjection row : results) {
            Long categoryId = row.categoryId();
            BigDecimal amount = row.totalAmount();
            int count = row.transactionCount();

            Category category = categoryId != null ? categoryMap.get(categoryId) : null;
            String categoryName = category != null ? category.getName() : "Uncategorized";
            String categoryColor = category != null ? category.getColor() : "#9CA3AF";

            BigDecimal percentage = totalSpending.compareTo(BigDecimal.ZERO) > 0 ?
                    amount.multiply(BigDecimal.valueOf(100))
                            .divide(totalSpending, 2, RoundingMode.HALF_UP) :
                    BigDecimal.ZERO;

            categories.add(SpendingByCategoryDto.CategorySpending.builder()
                    .categoryId(categoryId)
                    .categoryName(categoryName)
                    .categoryColor(categoryColor)
                    .amount(amount)
                    .percentage(percentage)
                    .transactionCount(count)
                    .build());
        }

        return SpendingByCategoryDto.builder()
                .totalSpending(totalSpending)
                .categories(categories)
                .build();
    }

    public TrendDto getTrends(Long userId, int months) {
        List<TrendDto.MonthlyTrend> trends = new ArrayList<>();

        YearMonth currentMonth = YearMonth.now();

        for (int i = months - 1; i >= 0; i--) {
            YearMonth month = currentMonth.minusMonths(i);
            LocalDate startDate = month.atDay(1);
            LocalDate endDate = month.atEndOfMonth();

            BigDecimal income = transactionAnalyticsRepository.sumByUserIdAndDateRangeAndType(
                    userId, startDate, endDate, true, true);

            BigDecimal expenses = transactionAnalyticsRepository.sumByUserIdAndDateRangeAndType(
                    userId, startDate, endDate, false, true).abs();

            BigDecimal transfers = transactionAnalyticsRepository.sumByUserIdAndDateRangeAndType(
                    userId, startDate, endDate, false, false)
                    .subtract(transactionAnalyticsRepository.sumByUserIdAndDateRangeAndType(
                            userId, startDate, endDate, false, true)).abs();

            BigDecimal netCashFlow = income.subtract(expenses);

            trends.add(TrendDto.MonthlyTrend.builder()
                    .month(month)
                    .income(income)
                    .expenses(expenses)
                    .transfers(transfers)
                    .netCashFlow(netCashFlow)
                    .build());
        }

        return TrendDto.builder()
                .trends(trends)
                .build();
    }

    public BudgetInsightsDto getBudgetInsights(Long userId, String monthRaw, Integer historyMonthsRaw) {
        YearMonth month = normalizeMonth(monthRaw);
        int historyMonths = normalizeHistoryMonths(historyMonthsRaw);

        LocalDate asOfDate = resolveAsOfDate(month);
        int asOfDay = asOfDate.getDayOfMonth();

        Map<Long, Category> categoryMap = categoryViewService.getEffectiveCategoryMapForUser(userId);
        Map<Long, BigDecimal> budgetByCategoryId = budgetTargetRepository.findByUserIdAndMonthKey(userId, month.toString()).stream()
                .collect(java.util.stream.Collectors.toMap(
                        target -> target.getCategoryId(),
                        target -> target.getTargetAmount(),
                        BigDecimal::add
                ));

        List<YearMonth> baselineMonths = IntStream.rangeClosed(1, historyMonths)
                .mapToObj(month::minusMonths)
                .toList();

        Map<YearMonth, Map<Long, BigDecimal>> fullMonthSpendByCategory = new HashMap<>();
        Map<YearMonth, Map<Long, BigDecimal>> monthToDateSpendByCategory = new HashMap<>();

        List<YearMonth> allMonths = new ArrayList<>();
        allMonths.add(month);
        allMonths.addAll(baselineMonths);

        for (YearMonth iterMonth : allMonths) {
            LocalDate fullStart = iterMonth.atDay(1);
            LocalDate fullEnd = iterMonth.atEndOfMonth();

            LocalDate mtdEnd = iterMonth.atDay(Math.min(asOfDay, iterMonth.lengthOfMonth()));

            fullMonthSpendByCategory.put(iterMonth, toCategoryAmountMap(
                    transactionAnalyticsRepository.sumByCategory(userId, fullStart, fullEnd)));
            monthToDateSpendByCategory.put(iterMonth, toCategoryAmountMap(
                    transactionAnalyticsRepository.sumByCategory(userId, fullStart, mtdEnd)));
        }

        Set<Long> relevantCategoryIds = new HashSet<>();
        categoryMap.values().stream()
                .filter(category -> category.getCategoryType() == CategoryType.EXPENSE)
                .map(Category::getId)
                .forEach(relevantCategoryIds::add);
        relevantCategoryIds.addAll(budgetByCategoryId.keySet());
        fullMonthSpendByCategory.values().forEach(map -> relevantCategoryIds.addAll(map.keySet()));
        monthToDateSpendByCategory.values().forEach(map -> relevantCategoryIds.addAll(map.keySet()));

        Map<Long, BigDecimal> currentFullMonthMap = fullMonthSpendByCategory.getOrDefault(month, Map.of());
        Map<Long, BigDecimal> currentMonthToDateMap = monthToDateSpendByCategory.getOrDefault(month, Map.of());

        List<BudgetInsightsDto.CategoryInsight> insights = new ArrayList<>();

        for (Long categoryId : relevantCategoryIds) {
            if (categoryId == null) {
                continue;
            }

            Category category = categoryMap.get(categoryId);
            if (category != null && category.getCategoryType() != CategoryType.EXPENSE) {
                continue;
            }

            BigDecimal currentBudget = budgetByCategoryId.getOrDefault(categoryId, BigDecimal.ZERO);
            BigDecimal currentMonthSpend = currentFullMonthMap.getOrDefault(categoryId, BigDecimal.ZERO);
            BigDecimal monthToDateSpend = currentMonthToDateMap.getOrDefault(categoryId, BigDecimal.ZERO);
            BigDecimal averageMonthlySpend = averageByCategory(baselineMonths, fullMonthSpendByCategory, categoryId);
            BigDecimal averageMonthToDateSpend = averageByCategory(baselineMonths, monthToDateSpendByCategory, categoryId);
            BigDecimal recommendedBudget = averageMonthlySpend;
            BigDecimal budgetDelta = recommendedBudget.subtract(currentBudget).setScale(2, RoundingMode.HALF_UP);
            BigDecimal monthToDateDelta = monthToDateSpend.subtract(averageMonthToDateSpend).setScale(2, RoundingMode.HALF_UP);

            if (currentBudget.compareTo(BigDecimal.ZERO) == 0
                    && currentMonthSpend.compareTo(BigDecimal.ZERO) == 0
                    && monthToDateSpend.compareTo(BigDecimal.ZERO) == 0
                    && averageMonthlySpend.compareTo(BigDecimal.ZERO) == 0
                    && averageMonthToDateSpend.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            insights.add(BudgetInsightsDto.CategoryInsight.builder()
                    .categoryId(categoryId)
                    .categoryName(category != null ? category.getName() : "Uncategorized")
                    .categoryColor(category != null ? category.getColor() : "#9CA3AF")
                    .currentBudget(currentBudget.setScale(2, RoundingMode.HALF_UP))
                    .currentMonthSpend(currentMonthSpend.setScale(2, RoundingMode.HALF_UP))
                    .averageMonthlySpend(averageMonthlySpend)
                    .recommendedBudget(recommendedBudget)
                    .budgetDelta(budgetDelta)
                    .budgetDeltaPct(computePercentageDelta(budgetDelta, currentBudget))
                    .monthToDateSpend(monthToDateSpend.setScale(2, RoundingMode.HALF_UP))
                    .averageMonthToDateSpend(averageMonthToDateSpend)
                    .monthToDateDelta(monthToDateDelta)
                    .monthToDateDeltaPct(computePercentageDelta(monthToDateDelta, averageMonthToDateSpend))
                    .build());
        }

        insights.sort(Comparator
                .comparing((BudgetInsightsDto.CategoryInsight insight) ->
                        insight.getCurrentMonthSpend()
                                .max(insight.getAverageMonthlySpend())
                                .max(insight.getCurrentBudget()))
                .reversed()
                .thenComparing(BudgetInsightsDto.CategoryInsight::getCategoryName));

        BigDecimal totalCurrentBudget = insights.stream()
                .map(BudgetInsightsDto.CategoryInsight::getCurrentBudget)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalRecommendedBudget = insights.stream()
                .map(BudgetInsightsDto.CategoryInsight::getRecommendedBudget)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        return BudgetInsightsDto.builder()
                .month(month.toString())
                .asOfDate(asOfDate)
                .historyMonths(historyMonths)
                .totalCurrentBudget(totalCurrentBudget)
                .totalRecommendedBudget(totalRecommendedBudget)
                .categories(insights)
                .build();
    }

    public CashFlowDto getCashFlow(Long userId, LocalDate startDate, LocalDate endDate) {
        if (startDate == null) {
            startDate = LocalDate.now().withDayOfMonth(1);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        BigDecimal totalIncome = transactionAnalyticsRepository.sumByUserIdAndDateRangeAndType(
                userId, startDate, endDate, true, true);

        BigDecimal totalExpenses = transactionAnalyticsRepository.sumByUserIdAndDateRangeAndType(
                userId, startDate, endDate, false, true).abs();

        BigDecimal allNegative = transactionAnalyticsRepository.sumByUserIdAndDateRangeAndType(
                userId, startDate, endDate, false, false).abs();
        BigDecimal totalTransfers = allNegative.subtract(totalExpenses);

        BigDecimal netCashFlow = totalIncome.subtract(totalExpenses);

        BigDecimal savingsRate = BigDecimal.ZERO;
        if (totalIncome.compareTo(BigDecimal.ZERO) > 0) {
            savingsRate = netCashFlow.multiply(BigDecimal.valueOf(100))
                    .divide(totalIncome, 2, RoundingMode.HALF_UP);
        }

        return CashFlowDto.builder()
                .startDate(startDate)
                .endDate(endDate)
                .totalIncome(totalIncome)
                .totalExpenses(totalExpenses)
                .totalTransfers(totalTransfers)
                .netCashFlow(netCashFlow)
                .savingsRate(savingsRate)
                .build();
    }

    private YearMonth normalizeMonth(String monthRaw) {
        if (monthRaw == null || monthRaw.isBlank()) {
            return YearMonth.now();
        }
        try {
            return YearMonth.parse(monthRaw);
        } catch (Exception ex) {
            throw ApiException.badRequest("month must be in YYYY-MM format");
        }
    }

    private int normalizeHistoryMonths(Integer historyMonthsRaw) {
        int historyMonths = historyMonthsRaw == null ? DEFAULT_HISTORY_MONTHS : historyMonthsRaw;
        if (historyMonths < MIN_HISTORY_MONTHS || historyMonths > MAX_HISTORY_MONTHS) {
            throw ApiException.badRequest("historyMonths must be between 1 and 24");
        }
        return historyMonths;
    }

    private LocalDate resolveAsOfDate(YearMonth month) {
        YearMonth currentMonth = YearMonth.now();
        if (month.equals(currentMonth)) {
            return LocalDate.now();
        }
        if (month.isBefore(currentMonth)) {
            return month.atEndOfMonth();
        }
        return month.atDay(1);
    }

    private BigDecimal averageByCategory(
            List<YearMonth> months,
            Map<YearMonth, Map<Long, BigDecimal>> amountsByMonth,
            Long categoryId) {
        if (months.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal sum = months.stream()
                .map(month -> amountsByMonth
                        .getOrDefault(month, Map.of())
                        .getOrDefault(categoryId, BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(months.size()), 2, RoundingMode.HALF_UP);
    }

    private Map<Long, BigDecimal> toCategoryAmountMap(
            List<TransactionAnalyticsRepository.CategorySpendingProjection> rows) {
        Map<Long, BigDecimal> values = new HashMap<>();
        for (TransactionAnalyticsRepository.CategorySpendingProjection row : rows) {
            if (row.categoryId() == null) {
                continue;
            }
            values.put(row.categoryId(), row.totalAmount().setScale(2, RoundingMode.HALF_UP));
        }
        return values;
    }

    private BigDecimal computePercentageDelta(BigDecimal deltaAmount, BigDecimal baselineAmount) {
        if (baselineAmount == null || baselineAmount.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return deltaAmount
                .multiply(BigDecimal.valueOf(100))
                .divide(baselineAmount, 2, RoundingMode.HALF_UP);
    }
}
