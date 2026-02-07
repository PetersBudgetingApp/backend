package com.peter.budget.service.recurring;

import com.peter.budget.model.entity.Transaction;
import com.peter.budget.model.enums.Frequency;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class RecurringPatternDetectionEngine {

    private static final int MIN_OCCURRENCES_FOR_PATTERN = 2;
    private static final double AMOUNT_VARIANCE_THRESHOLD = 0.1;

    public Map<String, List<Transaction>> groupByMerchant(List<Transaction> transactions) {
        return transactions.stream()
                .filter(transaction -> !transaction.isInternalTransfer() && transaction.getDescription() != null)
                .collect(Collectors.groupingBy(
                        transaction -> normalizeDescription(transaction.getDescription()),
                        Collectors.toList()
                ));
    }

    public Optional<DetectedPattern> analyze(List<Transaction> transactions) {
        if (transactions.size() < MIN_OCCURRENCES_FOR_PATTERN) {
            return Optional.empty();
        }

        List<Transaction> sorted = transactions.stream()
                .sorted(Comparator.comparing(Transaction::getPostedAt))
                .toList();

        List<Long> intervals = new ArrayList<>();
        for (int i = 1; i < sorted.size(); i++) {
            long days = ChronoUnit.DAYS.between(
                    sorted.get(i - 1).getPostedAt().atZone(ZoneOffset.UTC).toLocalDate(),
                    sorted.get(i).getPostedAt().atZone(ZoneOffset.UTC).toLocalDate()
            );
            intervals.add(days);
        }

        if (intervals.isEmpty()) {
            return Optional.empty();
        }

        double avgInterval = intervals.stream().mapToLong(Long::longValue).average().orElse(0);
        Frequency frequency = determineFrequency(avgInterval);
        if (frequency == null) {
            return Optional.empty();
        }

        BigDecimal averageAmount = transactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(transactions.size()), 2, RoundingMode.HALF_UP);

        BigDecimal maxVariance = transactions.stream()
                .map(transaction -> transaction.getAmount().subtract(averageAmount).abs())
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        if (averageAmount.abs().compareTo(BigDecimal.ZERO) > 0 &&
                maxVariance.divide(averageAmount.abs(), 2, RoundingMode.HALF_UP)
                        .compareTo(BigDecimal.valueOf(AMOUNT_VARIANCE_THRESHOLD)) > 0) {
            return Optional.empty();
        }

        Transaction lastTransaction = sorted.get(sorted.size() - 1);
        Integer dayOfMonth = lastTransaction.getPostedAt().atZone(ZoneOffset.UTC).getDayOfMonth();
        Integer dayOfWeek = frequency == Frequency.WEEKLY
                ? lastTransaction.getPostedAt().atZone(ZoneOffset.UTC).getDayOfWeek().getValue()
                : null;

        LocalDate nextExpected = calculateNextExpectedDate(
                lastTransaction.getPostedAt().atZone(ZoneOffset.UTC).toLocalDate(),
                frequency,
                dayOfMonth
        );

        Long categoryId = transactions.stream()
                .filter(transaction -> transaction.getCategoryId() != null)
                .collect(Collectors.groupingBy(Transaction::getCategoryId, Collectors.counting()))
                .entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        boolean isBill = averageAmount.compareTo(BigDecimal.ZERO) < 0;

        return Optional.of(new DetectedPattern(
                frequency,
                averageAmount.abs(),
                maxVariance,
                dayOfMonth,
                dayOfWeek,
                nextExpected,
                categoryId,
                isBill,
                lastTransaction.getPostedAt()
        ));
    }

    private String normalizeDescription(String description) {
        String normalized = description.toUpperCase()
                .replaceAll("[0-9]{4,}", "")
                .replaceAll("\\s+", " ")
                .replaceAll("[#*]+", "")
                .trim();

        if (normalized.length() > 50) {
            return normalized.substring(0, 50);
        }

        return normalized;
    }

    private Frequency determineFrequency(double averageDays) {
        if (averageDays >= 5 && averageDays <= 9) {
            return Frequency.WEEKLY;
        }
        if (averageDays >= 12 && averageDays <= 16) {
            return Frequency.BIWEEKLY;
        }
        if (averageDays >= 25 && averageDays <= 35) {
            return Frequency.MONTHLY;
        }
        if (averageDays >= 85 && averageDays <= 100) {
            return Frequency.QUARTERLY;
        }
        if (averageDays >= 350 && averageDays <= 380) {
            return Frequency.YEARLY;
        }
        return null;
    }

    private LocalDate calculateNextExpectedDate(LocalDate lastDate, Frequency frequency, Integer dayOfMonth) {
        LocalDate now = LocalDate.now();

        return switch (frequency) {
            case WEEKLY -> advanceUntilOnOrAfter(lastDate.plusWeeks(1), now, frequency);
            case BIWEEKLY -> advanceUntilOnOrAfter(lastDate.plusWeeks(2), now, frequency);
            case MONTHLY -> {
                YearMonth nextMonth = YearMonth.from(lastDate).plusMonths(1);
                while (nextMonth.atDay(Math.min(dayOfMonth, nextMonth.lengthOfMonth())).isBefore(now)) {
                    nextMonth = nextMonth.plusMonths(1);
                }
                yield nextMonth.atDay(Math.min(dayOfMonth, nextMonth.lengthOfMonth()));
            }
            case QUARTERLY -> advanceUntilOnOrAfter(lastDate.plusMonths(3), now, frequency);
            case YEARLY -> advanceUntilOnOrAfter(lastDate.plusYears(1), now, frequency);
        };
    }

    private LocalDate advanceUntilOnOrAfter(LocalDate value, LocalDate now, Frequency frequency) {
        LocalDate result = value;
        while (result.isBefore(now)) {
            result = switch (frequency) {
                case WEEKLY -> result.plusWeeks(1);
                case BIWEEKLY -> result.plusWeeks(2);
                case QUARTERLY -> result.plusMonths(3);
                case YEARLY -> result.plusYears(1);
                default -> result;
            };
        }
        return result;
    }

    public record DetectedPattern(
            Frequency frequency,
            BigDecimal expectedAmount,
            BigDecimal amountVariance,
            Integer dayOfMonth,
            Integer dayOfWeek,
            LocalDate nextExpectedDate,
            Long categoryId,
            boolean isBill,
            Instant lastOccurrenceAt
    ) {}
}
