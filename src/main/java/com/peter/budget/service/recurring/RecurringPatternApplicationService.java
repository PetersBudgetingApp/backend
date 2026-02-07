package com.peter.budget.service.recurring;

import com.peter.budget.model.entity.RecurringPattern;
import com.peter.budget.model.entity.Transaction;
import com.peter.budget.repository.AccountRepository;
import com.peter.budget.repository.RecurringPatternRepository;
import com.peter.budget.repository.TransactionReadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RecurringPatternApplicationService {

    private static final int MIN_OCCURRENCES_FOR_PATTERN = 2;

    private final RecurringPatternRepository patternRepository;
    private final TransactionReadRepository transactionReadRepository;
    private final AccountRepository accountRepository;
    private final RecurringPatternDetectionEngine detectionEngine;

    @Transactional
    public int detectRecurringPatterns(Long userId) {
        List<Transaction> transactions = new ArrayList<>();

        for (var account : accountRepository.findActiveByUserId(userId)) {
            transactions.addAll(transactionReadRepository.findByAccountId(account.getId()));
        }

        Map<String, List<Transaction>> groupedByMerchant = detectionEngine.groupByMerchant(transactions);
        int patternsDetected = 0;

        for (Map.Entry<String, List<Transaction>> entry : groupedByMerchant.entrySet()) {
            String merchant = entry.getKey();
            List<Transaction> merchantTransactions = entry.getValue();

            if (merchantTransactions.size() < MIN_OCCURRENCES_FOR_PATTERN) {
                continue;
            }

            var detectedPattern = detectionEngine.analyze(merchantTransactions);
            if (detectedPattern.isPresent()) {
                saveOrUpdatePattern(userId, merchant, detectedPattern.get(), merchantTransactions);
                patternsDetected++;
            }
        }

        return patternsDetected;
    }

    private void saveOrUpdatePattern(Long userId, String merchant, RecurringPatternDetectionEngine.DetectedPattern pattern,
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
            return;
        }

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
