package com.peter.budget.service.recurring;

import com.peter.budget.model.entity.Transaction;
import com.peter.budget.model.enums.Frequency;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecurringPatternDetectionEngineTest {

    private final RecurringPatternDetectionEngine engine = new RecurringPatternDetectionEngine();

    // --- groupByMerchant tests ---

    @Test
    void groupByMerchantGroupsTransactionsByNormalizedDescription() {
        Transaction t1 = txn(1L, "NETFLIX.COM #1234", "-15.99", "2026-01-01");
        Transaction t2 = txn(2L, "NETFLIX.COM #5678", "-15.99", "2026-02-01");
        Transaction t3 = txn(3L, "SPOTIFY USA", "-9.99", "2026-01-15");

        Map<String, List<Transaction>> groups = engine.groupByMerchant(List.of(t1, t2, t3));

        // NETFLIX.COM #1234 and #5678 should normalize to same key (long numbers stripped)
        assertEquals(2, groups.size());
        // Both Netflix transactions in one group
        boolean hasNetflixGroup = groups.values().stream().anyMatch(list -> list.size() == 2);
        assertTrue(hasNetflixGroup);
    }

    @Test
    void groupByMerchantExcludesInternalTransfers() {
        Transaction transfer = txn(1L, "Transfer", "-100.00", "2026-01-01");
        transfer.setInternalTransfer(true);
        Transaction normal = txn(2L, "Coffee Shop", "-5.00", "2026-01-01");

        Map<String, List<Transaction>> groups = engine.groupByMerchant(List.of(transfer, normal));

        assertEquals(1, groups.size());
        assertTrue(groups.values().stream().allMatch(list ->
                list.stream().noneMatch(Transaction::isInternalTransfer)));
    }

    @Test
    void groupByMerchantExcludesTransactionsWithNullDescription() {
        Transaction noDesc = txn(1L, null, "-10.00", "2026-01-01");
        Transaction withDesc = txn(2L, "STORE", "-10.00", "2026-01-01");

        Map<String, List<Transaction>> groups = engine.groupByMerchant(List.of(noDesc, withDesc));

        assertEquals(1, groups.size());
    }

    // --- analyze tests ---

    @Test
    void analyzeReturnsEmptyForFewerThanTwoTransactions() {
        Transaction single = txn(1L, "Netflix", "-15.99", "2026-01-01");

        Optional<RecurringPatternDetectionEngine.DetectedPattern> result = engine.analyze(List.of(single));

        assertTrue(result.isEmpty());
    }

    @Test
    void analyzeReturnsEmptyForEmptyList() {
        Optional<RecurringPatternDetectionEngine.DetectedPattern> result = engine.analyze(List.of());

        assertTrue(result.isEmpty());
    }

    @Test
    void analyzeDetectsMonthlyPattern() {
        // ~30 day intervals
        List<Transaction> monthly = List.of(
                txn(1L, "Netflix", "-15.99", "2025-10-01"),
                txn(2L, "Netflix", "-15.99", "2025-11-01"),
                txn(3L, "Netflix", "-15.99", "2025-12-01"),
                txn(4L, "Netflix", "-15.99", "2026-01-01")
        );

        Optional<RecurringPatternDetectionEngine.DetectedPattern> result = engine.analyze(monthly);

        assertTrue(result.isPresent());
        assertEquals(Frequency.MONTHLY, result.get().frequency());
        assertEquals(new BigDecimal("15.99"), result.get().expectedAmount());
        assertTrue(result.get().isBill()); // negative amounts are bills
        assertNotNull(result.get().nextExpectedDate());
        assertNotNull(result.get().dayOfMonth());
    }

    @Test
    void analyzeDetectsWeeklyPattern() {
        List<Transaction> weekly = List.of(
                txn(1L, "Gym", "-20.00", "2026-01-01"),
                txn(2L, "Gym", "-20.00", "2026-01-08"),
                txn(3L, "Gym", "-20.00", "2026-01-15"),
                txn(4L, "Gym", "-20.00", "2026-01-22")
        );

        Optional<RecurringPatternDetectionEngine.DetectedPattern> result = engine.analyze(weekly);

        assertTrue(result.isPresent());
        assertEquals(Frequency.WEEKLY, result.get().frequency());
        assertNotNull(result.get().dayOfWeek());
    }

    @Test
    void analyzeDetectsBiweeklyPattern() {
        List<Transaction> biweekly = List.of(
                txn(1L, "Payroll", "2500.00", "2025-12-01"),
                txn(2L, "Payroll", "2500.00", "2025-12-15"),
                txn(3L, "Payroll", "2500.00", "2025-12-29"),
                txn(4L, "Payroll", "2500.00", "2026-01-12")
        );

        Optional<RecurringPatternDetectionEngine.DetectedPattern> result = engine.analyze(biweekly);

        assertTrue(result.isPresent());
        assertEquals(Frequency.BIWEEKLY, result.get().frequency());
        assertFalse(result.get().isBill()); // positive amounts are not bills
    }

    @Test
    void analyzeDetectsQuarterlyPattern() {
        List<Transaction> quarterly = List.of(
                txn(1L, "Insurance", "-300.00", "2025-04-01"),
                txn(2L, "Insurance", "-300.00", "2025-07-01"),
                txn(3L, "Insurance", "-300.00", "2025-10-01"),
                txn(4L, "Insurance", "-300.00", "2026-01-01")
        );

        Optional<RecurringPatternDetectionEngine.DetectedPattern> result = engine.analyze(quarterly);

        assertTrue(result.isPresent());
        assertEquals(Frequency.QUARTERLY, result.get().frequency());
    }

    @Test
    void analyzeDetectsYearlyPattern() {
        List<Transaction> yearly = List.of(
                txn(1L, "Domain Renewal", "-12.99", "2024-01-15"),
                txn(2L, "Domain Renewal", "-12.99", "2025-01-15"),
                txn(3L, "Domain Renewal", "-12.99", "2026-01-15")
        );

        Optional<RecurringPatternDetectionEngine.DetectedPattern> result = engine.analyze(yearly);

        assertTrue(result.isPresent());
        assertEquals(Frequency.YEARLY, result.get().frequency());
    }

    @Test
    void analyzeReturnsEmptyForIrregularIntervals() {
        // Average interval between buckets (neither weekly, biweekly, monthly, quarterly, nor yearly)
        List<Transaction> irregular = List.of(
                txn(1L, "Random", "-50.00", "2026-01-01"),
                txn(2L, "Random", "-50.00", "2026-01-20"),
                txn(3L, "Random", "-50.00", "2026-02-08")
        );

        Optional<RecurringPatternDetectionEngine.DetectedPattern> result = engine.analyze(irregular);

        assertTrue(result.isEmpty());
    }

    @Test
    void analyzeReturnsEmptyWhenAmountVarianceTooHigh() {
        // Monthly-like intervals but wildly varying amounts
        List<Transaction> highVariance = List.of(
                txn(1L, "Utility", "-50.00", "2025-10-01"),
                txn(2L, "Utility", "-150.00", "2025-11-01"),
                txn(3L, "Utility", "-50.00", "2025-12-01"),
                txn(4L, "Utility", "-150.00", "2026-01-01")
        );

        Optional<RecurringPatternDetectionEngine.DetectedPattern> result = engine.analyze(highVariance);

        assertTrue(result.isEmpty());
    }

    @Test
    void analyzeInfersCategoryFromMostFrequentNonNullCategory() {
        Transaction t1 = txn(1L, "Netflix", "-15.99", "2025-10-01");
        t1.setCategoryId(10L);
        Transaction t2 = txn(2L, "Netflix", "-15.99", "2025-11-01");
        t2.setCategoryId(10L);
        Transaction t3 = txn(3L, "Netflix", "-15.99", "2025-12-01");
        t3.setCategoryId(20L);
        Transaction t4 = txn(4L, "Netflix", "-15.99", "2026-01-01");
        // no category

        Optional<RecurringPatternDetectionEngine.DetectedPattern> result = engine.analyze(List.of(t1, t2, t3, t4));

        assertTrue(result.isPresent());
        assertEquals(10L, result.get().categoryId()); // 10L appears twice, 20L once
    }

    @Test
    void analyzeReturnsNullCategoryWhenNoneSet() {
        List<Transaction> noCat = List.of(
                txn(1L, "Netflix", "-15.99", "2025-10-01"),
                txn(2L, "Netflix", "-15.99", "2025-11-01"),
                txn(3L, "Netflix", "-15.99", "2025-12-01")
        );

        Optional<RecurringPatternDetectionEngine.DetectedPattern> result = engine.analyze(noCat);

        assertTrue(result.isPresent());
        assertNull(result.get().categoryId());
    }

    @Test
    void analyzeCalculatesCorrectAverageAmount() {
        List<Transaction> txns = List.of(
                txn(1L, "Subscription", "-10.00", "2025-10-01"),
                txn(2L, "Subscription", "-10.00", "2025-11-01"),
                txn(3L, "Subscription", "-10.00", "2025-12-01")
        );

        Optional<RecurringPatternDetectionEngine.DetectedPattern> result = engine.analyze(txns);

        assertTrue(result.isPresent());
        assertEquals(new BigDecimal("10.00"), result.get().expectedAmount());
    }

    @Test
    void analyzeNextExpectedDateIsOnOrAfterToday() {
        List<Transaction> recent = List.of(
                txn(1L, "Sub", "-9.99", "2025-11-01"),
                txn(2L, "Sub", "-9.99", "2025-12-01"),
                txn(3L, "Sub", "-9.99", "2026-01-01")
        );

        Optional<RecurringPatternDetectionEngine.DetectedPattern> result = engine.analyze(recent);

        assertTrue(result.isPresent());
        assertFalse(result.get().nextExpectedDate().isBefore(LocalDate.now()));
    }

    private Transaction txn(Long id, String description, String amount, String date) {
        Instant posted = LocalDate.parse(date).atStartOfDay().toInstant(ZoneOffset.UTC);
        return Transaction.builder()
                .id(id)
                .accountId(1L)
                .description(description)
                .amount(new BigDecimal(amount))
                .postedAt(posted)
                .pending(false)
                .internalTransfer(false)
                .excludeFromTotals(false)
                .manuallyCategorized(false)
                .recurring(false)
                .build();
    }
}
