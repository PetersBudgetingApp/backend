package com.peter.budget.service.recurring;

import com.peter.budget.model.entity.Account;
import com.peter.budget.model.entity.RecurringPattern;
import com.peter.budget.model.entity.Transaction;
import com.peter.budget.model.enums.Frequency;
import com.peter.budget.repository.AccountRepository;
import com.peter.budget.repository.RecurringPatternRepository;
import com.peter.budget.repository.TransactionReadRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecurringPatternApplicationServiceTest {

    private static final long USER_ID = 7L;
    private static final long ACCOUNT_ID = 10L;

    @Mock
    private RecurringPatternRepository patternRepository;
    @Mock
    private TransactionReadRepository transactionReadRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private RecurringPatternDetectionEngine detectionEngine;

    @InjectMocks
    private RecurringPatternApplicationService applicationService;

    @Captor
    private ArgumentCaptor<RecurringPattern> patternCaptor;

    @Test
    void detectRecurringPatternsCreatesNewPatternFromDetection() {
        Account account = Account.builder().id(ACCOUNT_ID).userId(USER_ID).active(true).build();
        when(accountRepository.findActiveByUserId(USER_ID)).thenReturn(List.of(account));

        List<Transaction> transactions = List.of(
                txn(1L, "Netflix", "-15.99", "2025-11-01"),
                txn(2L, "Netflix", "-15.99", "2025-12-01"),
                txn(3L, "Netflix", "-15.99", "2026-01-01")
        );
        when(transactionReadRepository.findByAccountId(ACCOUNT_ID)).thenReturn(transactions);

        Map<String, List<Transaction>> grouped = Map.of("NETFLIX", transactions);
        when(detectionEngine.groupByMerchant(transactions)).thenReturn(grouped);

        RecurringPatternDetectionEngine.DetectedPattern detected = new RecurringPatternDetectionEngine.DetectedPattern(
                Frequency.MONTHLY, new BigDecimal("15.99"), BigDecimal.ZERO, 1, null,
                LocalDate.of(2026, 2, 1), 10L, true,
                Instant.parse("2026-01-01T00:00:00Z")
        );
        when(detectionEngine.analyze(transactions)).thenReturn(Optional.of(detected));
        when(patternRepository.findByMerchantPattern(USER_ID, "NETFLIX")).thenReturn(Optional.empty());
        when(patternRepository.save(any(RecurringPattern.class))).thenAnswer(i -> i.getArgument(0));

        int result = applicationService.detectRecurringPatterns(USER_ID);

        assertEquals(1, result);
        verify(patternRepository).save(patternCaptor.capture());
        RecurringPattern saved = patternCaptor.getValue();
        assertEquals(USER_ID, saved.getUserId());
        assertEquals("NETFLIX", saved.getMerchantPattern());
        assertEquals(Frequency.MONTHLY, saved.getFrequency());
        assertEquals(new BigDecimal("15.99"), saved.getExpectedAmount());
        assertTrue(saved.isBill());
        assertTrue(saved.isActive());
        assertEquals(10L, saved.getCategoryId());
    }

    @Test
    void detectRecurringPatternsUpdatesExistingPattern() {
        Account account = Account.builder().id(ACCOUNT_ID).userId(USER_ID).active(true).build();
        when(accountRepository.findActiveByUserId(USER_ID)).thenReturn(List.of(account));

        List<Transaction> transactions = List.of(
                txn(1L, "Spotify", "-9.99", "2025-11-01"),
                txn(2L, "Spotify", "-9.99", "2025-12-01"),
                txn(3L, "Spotify", "-9.99", "2026-01-01")
        );
        when(transactionReadRepository.findByAccountId(ACCOUNT_ID)).thenReturn(transactions);

        Map<String, List<Transaction>> grouped = Map.of("SPOTIFY", transactions);
        when(detectionEngine.groupByMerchant(transactions)).thenReturn(grouped);

        RecurringPatternDetectionEngine.DetectedPattern detected = new RecurringPatternDetectionEngine.DetectedPattern(
                Frequency.MONTHLY, new BigDecimal("9.99"), BigDecimal.ZERO, 1, null,
                LocalDate.of(2026, 2, 1), 20L, true,
                Instant.parse("2026-01-01T00:00:00Z")
        );
        when(detectionEngine.analyze(transactions)).thenReturn(Optional.of(detected));

        RecurringPattern existing = RecurringPattern.builder()
                .id(55L)
                .userId(USER_ID)
                .merchantPattern("SPOTIFY")
                .name("Spotify Premium")
                .frequency(Frequency.MONTHLY)
                .expectedAmount(new BigDecimal("8.99"))
                .categoryId(20L) // already set
                .active(true)
                .build();
        when(patternRepository.findByMerchantPattern(USER_ID, "SPOTIFY")).thenReturn(Optional.of(existing));
        when(patternRepository.save(any(RecurringPattern.class))).thenAnswer(i -> i.getArgument(0));

        int result = applicationService.detectRecurringPatterns(USER_ID);

        assertEquals(1, result);
        verify(patternRepository).save(patternCaptor.capture());
        RecurringPattern saved = patternCaptor.getValue();
        assertEquals(55L, saved.getId());
        assertEquals(new BigDecimal("9.99"), saved.getExpectedAmount()); // updated
        assertEquals(20L, saved.getCategoryId()); // preserved (was already set)
    }

    @Test
    void detectRecurringPatternsUpdatesCategoryOnExistingIfNull() {
        Account account = Account.builder().id(ACCOUNT_ID).userId(USER_ID).active(true).build();
        when(accountRepository.findActiveByUserId(USER_ID)).thenReturn(List.of(account));

        List<Transaction> transactions = List.of(
                txn(1L, "Service", "-25.00", "2025-11-01"),
                txn(2L, "Service", "-25.00", "2025-12-01")
        );
        when(transactionReadRepository.findByAccountId(ACCOUNT_ID)).thenReturn(transactions);
        when(detectionEngine.groupByMerchant(transactions)).thenReturn(Map.of("SERVICE", transactions));

        RecurringPatternDetectionEngine.DetectedPattern detected = new RecurringPatternDetectionEngine.DetectedPattern(
                Frequency.MONTHLY, new BigDecimal("25.00"), BigDecimal.ZERO, 1, null,
                LocalDate.of(2026, 2, 1), 30L, true,
                Instant.parse("2025-12-01T00:00:00Z")
        );
        when(detectionEngine.analyze(transactions)).thenReturn(Optional.of(detected));

        RecurringPattern existing = RecurringPattern.builder()
                .id(66L).userId(USER_ID).merchantPattern("SERVICE")
                .name("Service").categoryId(null).active(true).build();
        when(patternRepository.findByMerchantPattern(USER_ID, "SERVICE")).thenReturn(Optional.of(existing));
        when(patternRepository.save(any(RecurringPattern.class))).thenAnswer(i -> i.getArgument(0));

        applicationService.detectRecurringPatterns(USER_ID);

        verify(patternRepository).save(patternCaptor.capture());
        assertEquals(30L, patternCaptor.getValue().getCategoryId()); // updated from null
    }

    @Test
    void detectRecurringPatternsSkipsGroupsWithFewerThanTwoTransactions() {
        Account account = Account.builder().id(ACCOUNT_ID).userId(USER_ID).active(true).build();
        when(accountRepository.findActiveByUserId(USER_ID)).thenReturn(List.of(account));

        Transaction single = txn(1L, "One-off", "-99.00", "2026-01-01");
        when(transactionReadRepository.findByAccountId(ACCOUNT_ID)).thenReturn(List.of(single));
        when(detectionEngine.groupByMerchant(List.of(single))).thenReturn(Map.of("ONE-OFF", List.of(single)));

        int result = applicationService.detectRecurringPatterns(USER_ID);

        assertEquals(0, result);
        verify(patternRepository, never()).save(any(RecurringPattern.class));
    }

    @Test
    void detectRecurringPatternsReturnsZeroWhenNoAccounts() {
        when(accountRepository.findActiveByUserId(USER_ID)).thenReturn(List.of());

        int result = applicationService.detectRecurringPatterns(USER_ID);

        assertEquals(0, result);
    }

    @Test
    void detectRecurringPatternsSkipsWhenAnalysisReturnsEmpty() {
        Account account = Account.builder().id(ACCOUNT_ID).userId(USER_ID).active(true).build();
        when(accountRepository.findActiveByUserId(USER_ID)).thenReturn(List.of(account));

        List<Transaction> transactions = List.of(
                txn(1L, "Random", "-50.00", "2026-01-01"),
                txn(2L, "Random", "-50.00", "2026-01-20")
        );
        when(transactionReadRepository.findByAccountId(ACCOUNT_ID)).thenReturn(transactions);
        when(detectionEngine.groupByMerchant(transactions)).thenReturn(Map.of("RANDOM", transactions));
        when(detectionEngine.analyze(transactions)).thenReturn(Optional.empty());

        int result = applicationService.detectRecurringPatterns(USER_ID);

        assertEquals(0, result);
        verify(patternRepository, never()).save(any(RecurringPattern.class));
    }

    @Test
    void detectRecurringPatternsTruncatesLongDescriptionForName() {
        Account account = Account.builder().id(ACCOUNT_ID).userId(USER_ID).active(true).build();
        when(accountRepository.findActiveByUserId(USER_ID)).thenReturn(List.of(account));

        String longDesc = "A".repeat(150);
        List<Transaction> transactions = List.of(
                txn(1L, longDesc, "-10.00", "2025-11-01"),
                txn(2L, longDesc, "-10.00", "2025-12-01")
        );
        when(transactionReadRepository.findByAccountId(ACCOUNT_ID)).thenReturn(transactions);
        when(detectionEngine.groupByMerchant(transactions)).thenReturn(Map.of("LONG", transactions));

        RecurringPatternDetectionEngine.DetectedPattern detected = new RecurringPatternDetectionEngine.DetectedPattern(
                Frequency.MONTHLY, new BigDecimal("10.00"), BigDecimal.ZERO, 1, null,
                LocalDate.of(2026, 2, 1), null, true,
                Instant.parse("2025-12-01T00:00:00Z")
        );
        when(detectionEngine.analyze(transactions)).thenReturn(Optional.of(detected));
        when(patternRepository.findByMerchantPattern(USER_ID, "LONG")).thenReturn(Optional.empty());
        when(patternRepository.save(any(RecurringPattern.class))).thenAnswer(i -> i.getArgument(0));

        applicationService.detectRecurringPatterns(USER_ID);

        verify(patternRepository).save(patternCaptor.capture());
        assertEquals(100, patternCaptor.getValue().getName().length());
    }

    private Transaction txn(Long id, String description, String amount, String date) {
        Instant posted = LocalDate.parse(date).atStartOfDay().toInstant(ZoneOffset.UTC);
        return Transaction.builder()
                .id(id)
                .accountId(ACCOUNT_ID)
                .description(description)
                .amount(new BigDecimal(amount))
                .postedAt(posted)
                .pending(false)
                .internalTransfer(false)
                .build();
    }
}
