package com.peter.budget.service;

import com.peter.budget.exception.ApiException;
import com.peter.budget.model.dto.CategoryDto;
import com.peter.budget.model.dto.RecurringPatternDto;
import com.peter.budget.model.dto.UpcomingBillDto;
import com.peter.budget.model.entity.Category;
import com.peter.budget.model.entity.RecurringPattern;
import com.peter.budget.model.entity.Transaction;
import com.peter.budget.model.enums.Frequency;
import com.peter.budget.repository.AccountRepository;
import com.peter.budget.repository.CategoryRepository;
import com.peter.budget.repository.RecurringPatternRepository;
import com.peter.budget.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecurringDetectionService {

    private static final int MIN_OCCURRENCES_FOR_PATTERN = 2;
    private static final double AMOUNT_VARIANCE_THRESHOLD = 0.1;

    private final RecurringPatternRepository patternRepository;
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;

    @Transactional
    public int detectRecurringPatterns(Long userId) {
        List<Transaction> transactions = new ArrayList<>();

        for (var account : accountRepository.findActiveByUserId(userId)) {
            transactions.addAll(transactionRepository.findByAccountId(account.getId()));
        }

        Map<String, List<Transaction>> groupedByMerchant = transactions.stream()
                .filter(t -> !t.isInternalTransfer() && t.getDescription() != null)
                .collect(Collectors.groupingBy(
                        t -> normalizeDescription(t.getDescription()),
                        Collectors.toList()
                ));

        int patternsDetected = 0;

        for (Map.Entry<String, List<Transaction>> entry : groupedByMerchant.entrySet()) {
            String merchant = entry.getKey();
            List<Transaction> txList = entry.getValue();

            if (txList.size() < MIN_OCCURRENCES_FOR_PATTERN) {
                continue;
            }

            DetectedPattern pattern = analyzeTransactionPattern(txList);
            if (pattern != null) {
                saveOrUpdatePattern(userId, merchant, pattern, txList);
                patternsDetected++;
            }
        }

        return patternsDetected;
    }

    private String normalizeDescription(String description) {
        if (description == null) return "";

        String normalized = description.toUpperCase()
                .replaceAll("[0-9]{4,}", "")
                .replaceAll("\\s+", " ")
                .replaceAll("[#*]+", "")
                .trim();

        if (normalized.length() > 50) {
            normalized = normalized.substring(0, 50);
        }

        return normalized;
    }

    private DetectedPattern analyzeTransactionPattern(List<Transaction> transactions) {
        if (transactions.size() < MIN_OCCURRENCES_FOR_PATTERN) {
            return null;
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
            return null;
        }

        double avgInterval = intervals.stream().mapToLong(Long::longValue).average().orElse(0);

        Frequency frequency = determineFrequency(avgInterval);
        if (frequency == null) {
            return null;
        }

        BigDecimal avgAmount = transactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(transactions.size()), 2, RoundingMode.HALF_UP);

        BigDecimal maxVariance = transactions.stream()
                .map(t -> t.getAmount().subtract(avgAmount).abs())
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        if (avgAmount.abs().compareTo(BigDecimal.ZERO) > 0 &&
                maxVariance.divide(avgAmount.abs(), 2, RoundingMode.HALF_UP)
                        .compareTo(BigDecimal.valueOf(AMOUNT_VARIANCE_THRESHOLD)) > 0) {
            return null;
        }

        Transaction lastTx = sorted.get(sorted.size() - 1);
        Integer dayOfMonth = lastTx.getPostedAt().atZone(ZoneOffset.UTC).getDayOfMonth();
        Integer dayOfWeek = frequency == Frequency.WEEKLY ?
                lastTx.getPostedAt().atZone(ZoneOffset.UTC).getDayOfWeek().getValue() : null;

        LocalDate nextExpected = calculateNextExpectedDate(
                lastTx.getPostedAt().atZone(ZoneOffset.UTC).toLocalDate(),
                frequency,
                dayOfMonth,
                dayOfWeek
        );

        Long categoryId = transactions.stream()
                .filter(t -> t.getCategoryId() != null)
                .collect(Collectors.groupingBy(Transaction::getCategoryId, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        boolean isBill = avgAmount.compareTo(BigDecimal.ZERO) < 0;

        return new DetectedPattern(
                frequency,
                avgAmount.abs(),
                maxVariance,
                dayOfMonth,
                dayOfWeek,
                nextExpected,
                categoryId,
                isBill,
                lastTx.getPostedAt()
        );
    }

    private Frequency determineFrequency(double avgDays) {
        if (avgDays >= 5 && avgDays <= 9) {
            return Frequency.WEEKLY;
        } else if (avgDays >= 12 && avgDays <= 16) {
            return Frequency.BIWEEKLY;
        } else if (avgDays >= 25 && avgDays <= 35) {
            return Frequency.MONTHLY;
        } else if (avgDays >= 85 && avgDays <= 100) {
            return Frequency.QUARTERLY;
        } else if (avgDays >= 350 && avgDays <= 380) {
            return Frequency.YEARLY;
        }
        return null;
    }

    private LocalDate calculateNextExpectedDate(LocalDate lastDate, Frequency frequency,
                                                  Integer dayOfMonth, Integer dayOfWeek) {
        LocalDate now = LocalDate.now();

        return switch (frequency) {
            case WEEKLY -> {
                LocalDate next = lastDate.plusWeeks(1);
                while (next.isBefore(now)) {
                    next = next.plusWeeks(1);
                }
                yield next;
            }
            case BIWEEKLY -> {
                LocalDate next = lastDate.plusWeeks(2);
                while (next.isBefore(now)) {
                    next = next.plusWeeks(2);
                }
                yield next;
            }
            case MONTHLY -> {
                YearMonth nextMonth = YearMonth.from(lastDate).plusMonths(1);
                while (nextMonth.atDay(Math.min(dayOfMonth, nextMonth.lengthOfMonth())).isBefore(now)) {
                    nextMonth = nextMonth.plusMonths(1);
                }
                yield nextMonth.atDay(Math.min(dayOfMonth, nextMonth.lengthOfMonth()));
            }
            case QUARTERLY -> {
                LocalDate next = lastDate.plusMonths(3);
                while (next.isBefore(now)) {
                    next = next.plusMonths(3);
                }
                yield next;
            }
            case YEARLY -> {
                LocalDate next = lastDate.plusYears(1);
                while (next.isBefore(now)) {
                    next = next.plusYears(1);
                }
                yield next;
            }
        };
    }

    private void saveOrUpdatePattern(Long userId, String merchant, DetectedPattern pattern,
                                       List<Transaction> transactions) {
        RecurringPattern existing = patternRepository.findByMerchantPattern(userId, merchant).orElse(null);

        if (existing != null) {
            existing.setExpectedAmount(pattern.expectedAmount());
            existing.setAmountVariance(pattern.amountVariance());
            existing.setFrequency(pattern.frequency());
            existing.setDayOfMonth(pattern.dayOfMonth());
            existing.setDayOfWeek(pattern.dayOfWeek());
            existing.setNextExpectedDate(pattern.nextExpectedDate());
            existing.setLastOccurrenceAt(pattern.lastOccurrenceAt());
            if (existing.getCategoryId() == null && pattern.categoryId() != null) {
                existing.setCategoryId(pattern.categoryId());
            }
            patternRepository.save(existing);
        } else {
            String name = transactions.get(0).getDescription();
            if (name != null && name.length() > 100) {
                name = name.substring(0, 100);
            }

            RecurringPattern newPattern = RecurringPattern.builder()
                    .userId(userId)
                    .name(name != null ? name : merchant)
                    .merchantPattern(merchant)
                    .expectedAmount(pattern.expectedAmount())
                    .amountVariance(pattern.amountVariance())
                    .frequency(pattern.frequency())
                    .dayOfMonth(pattern.dayOfMonth())
                    .dayOfWeek(pattern.dayOfWeek())
                    .nextExpectedDate(pattern.nextExpectedDate())
                    .categoryId(pattern.categoryId())
                    .bill(pattern.isBill())
                    .active(true)
                    .lastOccurrenceAt(pattern.lastOccurrenceAt())
                    .build();
            patternRepository.save(newPattern);
        }
    }

    public List<RecurringPatternDto> getRecurringPatterns(Long userId) {
        return patternRepository.findActiveByUserId(userId).stream()
                .map(this::toDto)
                .toList();
    }

    public List<UpcomingBillDto> getUpcomingBills(Long userId, int days) {
        LocalDate now = LocalDate.now();
        LocalDate endDate = now.plusDays(days);

        return patternRepository.findUpcomingBills(userId, now.minusDays(7), endDate).stream()
                .map(pattern -> {
                    boolean overdue = pattern.getNextExpectedDate() != null &&
                            pattern.getNextExpectedDate().isBefore(now);
                    int daysUntilDue = pattern.getNextExpectedDate() != null ?
                            (int) ChronoUnit.DAYS.between(now, pattern.getNextExpectedDate()) : 0;

                    CategoryDto categoryDto = null;
                    if (pattern.getCategoryId() != null) {
                        categoryDto = categoryRepository.findById(pattern.getCategoryId())
                                .map(this::toCategoryDto)
                                .orElse(null);
                    }

                    return UpcomingBillDto.builder()
                            .patternId(pattern.getId())
                            .name(pattern.getName())
                            .expectedAmount(pattern.getExpectedAmount())
                            .dueDate(pattern.getNextExpectedDate())
                            .daysUntilDue(daysUntilDue)
                            .category(categoryDto)
                            .overdue(overdue)
                            .build();
                })
                .sorted(Comparator.comparing(UpcomingBillDto::getDueDate,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    public List<UpcomingBillDto> getBillsForMonth(Long userId, int year, int month) {
        return patternRepository.findBillsForMonth(userId, year, month).stream()
                .map(pattern -> {
                    LocalDate now = LocalDate.now();
                    boolean overdue = pattern.getNextExpectedDate() != null &&
                            pattern.getNextExpectedDate().isBefore(now);
                    int daysUntilDue = pattern.getNextExpectedDate() != null ?
                            (int) ChronoUnit.DAYS.between(now, pattern.getNextExpectedDate()) : 0;

                    CategoryDto categoryDto = null;
                    if (pattern.getCategoryId() != null) {
                        categoryDto = categoryRepository.findById(pattern.getCategoryId())
                                .map(this::toCategoryDto)
                                .orElse(null);
                    }

                    return UpcomingBillDto.builder()
                            .patternId(pattern.getId())
                            .name(pattern.getName())
                            .expectedAmount(pattern.getExpectedAmount())
                            .dueDate(pattern.getNextExpectedDate())
                            .daysUntilDue(daysUntilDue)
                            .category(categoryDto)
                            .overdue(overdue)
                            .build();
                })
                .sorted(Comparator.comparing(UpcomingBillDto::getDueDate,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    @Transactional
    public void deletePattern(Long userId, Long patternId) {
        RecurringPattern pattern = patternRepository.findByIdAndUserId(patternId, userId)
                .orElseThrow(() -> ApiException.notFound("Pattern not found"));
        patternRepository.deleteById(pattern.getId());
    }

    @Transactional
    public RecurringPatternDto togglePatternActive(Long userId, Long patternId, boolean active) {
        RecurringPattern pattern = patternRepository.findByIdAndUserId(patternId, userId)
                .orElseThrow(() -> ApiException.notFound("Pattern not found"));
        pattern.setActive(active);
        pattern = patternRepository.save(pattern);
        return toDto(pattern);
    }

    private RecurringPatternDto toDto(RecurringPattern pattern) {
        CategoryDto categoryDto = null;
        if (pattern.getCategoryId() != null) {
            categoryDto = categoryRepository.findById(pattern.getCategoryId())
                    .map(this::toCategoryDto)
                    .orElse(null);
        }

        return RecurringPatternDto.builder()
                .id(pattern.getId())
                .name(pattern.getName())
                .merchantPattern(pattern.getMerchantPattern())
                .expectedAmount(pattern.getExpectedAmount())
                .frequency(pattern.getFrequency())
                .dayOfMonth(pattern.getDayOfMonth())
                .nextExpectedDate(pattern.getNextExpectedDate())
                .category(categoryDto)
                .bill(pattern.isBill())
                .active(pattern.isActive())
                .lastOccurrenceAt(pattern.getLastOccurrenceAt())
                .build();
    }

    private CategoryDto toCategoryDto(Category category) {
        return CategoryDto.builder()
                .id(category.getId())
                .name(category.getName())
                .icon(category.getIcon())
                .color(category.getColor())
                .categoryType(category.getCategoryType())
                .build();
    }

    private record DetectedPattern(
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
