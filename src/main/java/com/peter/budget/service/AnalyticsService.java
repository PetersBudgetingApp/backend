package com.peter.budget.service;

import com.peter.budget.model.dto.CashFlowDto;
import com.peter.budget.model.dto.SpendingByCategoryDto;
import com.peter.budget.model.dto.TrendDto;
import com.peter.budget.model.entity.Category;
import com.peter.budget.repository.CategoryRepository;
import com.peter.budget.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;

    public SpendingByCategoryDto getSpendingByCategory(Long userId, LocalDate startDate, LocalDate endDate) {
        if (startDate == null) {
            startDate = LocalDate.now().withDayOfMonth(1);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        List<Object[]> results = transactionRepository.sumByCategory(userId, startDate, endDate);

        Map<Long, Category> categoryMap = categoryRepository.findByUserId(userId).stream()
                .collect(Collectors.toMap(Category::getId, Function.identity()));

        BigDecimal totalSpending = results.stream()
                .map(row -> (BigDecimal) row[1])
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<SpendingByCategoryDto.CategorySpending> categories = new ArrayList<>();

        for (Object[] row : results) {
            Long categoryId = (Long) row[0];
            BigDecimal amount = (BigDecimal) row[1];
            int count = (int) row[2];

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

            BigDecimal income = transactionRepository.sumByUserIdAndDateRangeAndType(
                    userId, startDate, endDate, true, true);

            BigDecimal expenses = transactionRepository.sumByUserIdAndDateRangeAndType(
                    userId, startDate, endDate, false, true).abs();

            BigDecimal transfers = transactionRepository.sumByUserIdAndDateRangeAndType(
                    userId, startDate, endDate, false, false)
                    .subtract(transactionRepository.sumByUserIdAndDateRangeAndType(
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

    public CashFlowDto getCashFlow(Long userId, LocalDate startDate, LocalDate endDate) {
        if (startDate == null) {
            startDate = LocalDate.now().withDayOfMonth(1);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        BigDecimal totalIncome = transactionRepository.sumByUserIdAndDateRangeAndType(
                userId, startDate, endDate, true, true);

        BigDecimal totalExpenses = transactionRepository.sumByUserIdAndDateRangeAndType(
                userId, startDate, endDate, false, true).abs();

        BigDecimal allNegative = transactionRepository.sumByUserIdAndDateRangeAndType(
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
}
