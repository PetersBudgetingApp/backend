package com.peter.budget.service;

import com.peter.budget.exception.ApiException;
import com.peter.budget.model.dto.TransactionCreateRequest;
import com.peter.budget.model.dto.TransactionDto;
import com.peter.budget.model.dto.TransactionUpdateRequest;
import com.peter.budget.model.entity.Account;
import com.peter.budget.model.entity.CategorizationRule;
import com.peter.budget.model.entity.Category;
import com.peter.budget.model.entity.Transaction;
import com.peter.budget.model.enums.AccountType;
import com.peter.budget.model.enums.CategoryType;
import com.peter.budget.repository.AccountRepository;
import com.peter.budget.repository.CategorizationRuleRepository;
import com.peter.budget.repository.TransactionReadRepository;
import com.peter.budget.repository.TransactionWriteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    private static final long USER_ID = 7L;
    private static final long TRANSACTION_ID = 11L;
    private static final long ACCOUNT_ID = 101L;

    @Mock
    private TransactionReadRepository transactionReadRepository;
    @Mock
    private TransactionWriteRepository transactionWriteRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private CategorizationRuleRepository categorizationRuleRepository;
    @Mock
    private AutoCategorizationService autoCategorizationService;
    @Mock
    private CategoryViewService categoryViewService;
    @Mock
    private TransferDetectionService transferDetectionService;

    @InjectMocks
    private TransactionService transactionService;

    @Captor
    private ArgumentCaptor<Transaction> transactionCaptor;

    @BeforeEach
    void setUp() {
        lenient().when(transactionWriteRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(accountRepository.findById(ACCOUNT_ID))
                .thenReturn(Optional.of(Account.builder()
                        .id(ACCOUNT_ID)
                        .name("Checking")
                        .accountType(AccountType.CHECKING)
                        .build()));
        lenient().when(categoryViewService.getEffectiveCategoryMapForUser(USER_ID))
                .thenReturn(Map.of());
    }

    @Test
    void createTransactionCreatesManuallyCategorizedTransactionWhenCategoryProvided() {
        Account account = Account.builder()
                .id(ACCOUNT_ID)
                .userId(USER_ID)
                .name("Checking")
                .accountType(AccountType.CHECKING)
                .build();
        Category groceries = Category.builder()
                .id(42L)
                .name("Groceries")
                .categoryType(CategoryType.EXPENSE)
                .system(false)
                .build();

        when(accountRepository.findByIdAndUserId(ACCOUNT_ID, USER_ID)).thenReturn(Optional.of(account));
        when(categoryViewService.getEffectiveCategoryByIdForUser(USER_ID, 42L)).thenReturn(Optional.of(groceries));
        when(categoryViewService.getEffectiveCategoryMapForUser(USER_ID)).thenReturn(Map.of(42L, groceries));

        TransactionCreateRequest request = new TransactionCreateRequest();
        request.setAccountId(ACCOUNT_ID);
        request.setPostedDate(LocalDate.parse("2026-02-05"));
        request.setTransactedDate(LocalDate.parse("2026-02-04"));
        request.setAmount(new BigDecimal("-74.33"));
        request.setDescription(" Grocery Store ");
        request.setPayee(" Main Street Market ");
        request.setMemo("Weekly run");
        request.setCategoryId(42L);
        request.setPending(true);
        request.setExcludeFromTotals(true);
        request.setNotes("Added manually");

        TransactionDto result = transactionService.createTransaction(USER_ID, request);

        verify(transactionWriteRepository).save(transactionCaptor.capture());
        Transaction saved = transactionCaptor.getValue();
        assertEquals(ACCOUNT_ID, saved.getAccountId());
        assertEquals(Instant.parse("2026-02-05T12:00:00Z"), saved.getPostedAt());
        assertEquals(Instant.parse("2026-02-04T12:00:00Z"), saved.getTransactedAt());
        assertEquals(new BigDecimal("-74.33"), saved.getAmount());
        assertEquals("Grocery Store", saved.getDescription());
        assertEquals("Main Street Market", saved.getPayee());
        assertEquals(42L, saved.getCategoryId());
        assertTrue(saved.isManuallyCategorized());
        assertNull(saved.getCategorizedByRuleId());
        assertTrue(saved.isPending());
        assertTrue(saved.isExcludeFromTotals());
        assertEquals("Added manually", saved.getNotes());
        assertNotNull(result.getCategory());
        assertEquals(42L, result.getCategory().getId());
    }

    @Test
    void createTransactionAutoCategorizesWhenCategoryNotProvided() {
        Account account = Account.builder()
                .id(ACCOUNT_ID)
                .userId(USER_ID)
                .name("Checking")
                .accountType(AccountType.CHECKING)
                .build();
        Category autoCategory = Category.builder()
                .id(77L)
                .name("Coffee")
                .categoryType(CategoryType.EXPENSE)
                .system(false)
                .build();

        when(accountRepository.findByIdAndUserId(ACCOUNT_ID, USER_ID)).thenReturn(Optional.of(account));
        when(autoCategorizationService.categorize(USER_ID, ACCOUNT_ID, new BigDecimal("-8.50"), "Coffee shop", null, null))
                .thenReturn(new AutoCategorizationService.CategorizationMatch(900L, 77L));
        when(categoryViewService.getEffectiveCategoryMapForUser(USER_ID)).thenReturn(Map.of(77L, autoCategory));

        TransactionCreateRequest request = new TransactionCreateRequest();
        request.setAccountId(ACCOUNT_ID);
        request.setPostedDate(LocalDate.parse("2026-02-06"));
        request.setAmount(new BigDecimal("-8.50"));
        request.setDescription("Coffee shop");

        TransactionDto result = transactionService.createTransaction(USER_ID, request);

        verify(transactionWriteRepository).save(transactionCaptor.capture());
        Transaction saved = transactionCaptor.getValue();
        assertEquals(77L, saved.getCategoryId());
        assertEquals(900L, saved.getCategorizedByRuleId());
        assertFalse(saved.isManuallyCategorized());
        assertNotNull(result.getCategory());
        assertEquals(77L, result.getCategory().getId());
    }

    @Test
    void createTransactionRejectsZeroAmount() {
        when(accountRepository.findByIdAndUserId(ACCOUNT_ID, USER_ID)).thenReturn(Optional.of(Account.builder()
                .id(ACCOUNT_ID)
                .userId(USER_ID)
                .build()));

        TransactionCreateRequest request = new TransactionCreateRequest();
        request.setAccountId(ACCOUNT_ID);
        request.setPostedDate(LocalDate.parse("2026-02-06"));
        request.setAmount(BigDecimal.ZERO);
        request.setDescription("Invalid amount");

        ApiException exception = assertThrows(
                ApiException.class,
                () -> transactionService.createTransaction(USER_ID, request)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        verify(transactionWriteRepository, never()).save(any(Transaction.class));
    }

    @Test
    void updateTransactionClearsCategoryWhenExplicitNullProvided() {
        Transaction existing = baseTransaction();
        existing.setCategoryId(5L);
        existing.setCategorizedByRuleId(88L);
        existing.setManuallyCategorized(true);

        when(transactionReadRepository.findByIdAndUserId(TRANSACTION_ID, USER_ID))
                .thenReturn(Optional.of(existing));

        TransactionUpdateRequest request = new TransactionUpdateRequest();
        request.setCategoryId(null);

        TransactionDto result = transactionService.updateTransaction(USER_ID, TRANSACTION_ID, request);

        verify(transactionWriteRepository).save(transactionCaptor.capture());
        Transaction saved = transactionCaptor.getValue();
        assertNull(saved.getCategoryId());
        assertNull(saved.getCategorizedByRuleId());
        assertFalse(saved.isManuallyCategorized());
        assertFalse(result.isManuallyCategorized());
        assertNull(result.getCategory());
    }

    @Test
    void updateTransactionKeepsCategoryWhenCategoryFieldMissing() {
        Transaction existing = baseTransaction();
        existing.setCategoryId(5L);
        existing.setManuallyCategorized(true);

        when(transactionReadRepository.findByIdAndUserId(TRANSACTION_ID, USER_ID))
                .thenReturn(Optional.of(existing));
        when(categoryViewService.getEffectiveCategoryMapForUser(USER_ID))
                .thenReturn(Map.of(5L, Category.builder()
                        .id(5L)
                        .name("Food")
                        .categoryType(CategoryType.EXPENSE)
                        .system(false)
                        .build()));

        TransactionUpdateRequest request = new TransactionUpdateRequest();

        TransactionDto result = transactionService.updateTransaction(USER_ID, TRANSACTION_ID, request);

        verify(transactionWriteRepository).save(transactionCaptor.capture());
        Transaction saved = transactionCaptor.getValue();
        assertEquals(5L, saved.getCategoryId());
        assertTrue(saved.isManuallyCategorized());
        assertNotNull(result.getCategory());
        assertEquals(5L, result.getCategory().getId());
        verify(categoryViewService, never()).getEffectiveCategoryByIdForUser(any(Long.class), any(Long.class));
    }

    @Test
    void updateTransactionAssignsCategoryWhenCategoryProvided() {
        Transaction existing = baseTransaction();
        existing.setCategorizedByRuleId(77L);

        Category groceries = Category.builder()
                .id(42L)
                .name("Groceries")
                .categoryType(CategoryType.EXPENSE)
                .system(false)
                .build();

        when(transactionReadRepository.findByIdAndUserId(TRANSACTION_ID, USER_ID))
                .thenReturn(Optional.of(existing));
        when(categoryViewService.getEffectiveCategoryByIdForUser(USER_ID, 42L))
                .thenReturn(Optional.of(groceries));
        when(categoryViewService.getEffectiveCategoryMapForUser(USER_ID))
                .thenReturn(Map.of(42L, groceries));

        TransactionUpdateRequest request = new TransactionUpdateRequest();
        request.setCategoryId(42L);

        TransactionDto result = transactionService.updateTransaction(USER_ID, TRANSACTION_ID, request);

        verify(transactionWriteRepository).save(transactionCaptor.capture());
        Transaction saved = transactionCaptor.getValue();
        assertEquals(42L, saved.getCategoryId());
        assertNull(saved.getCategorizedByRuleId());
        assertTrue(saved.isManuallyCategorized());
        assertNotNull(result.getCategory());
        assertEquals(42L, result.getCategory().getId());
    }

    @Test
    void getTransactionsForCategorizationRuleReturnsOnlyTrackedTransactions() {
        CategorizationRule rule = CategorizationRule.builder()
                .id(555L)
                .userId(USER_ID)
                .name("Coffee rule")
                .build();

        Transaction tracked = baseTransaction();
        tracked.setId(900L);
        tracked.setCategorizedByRuleId(rule.getId());
        tracked.setCategoryId(12L);

        when(categorizationRuleRepository.findByIdAndUserId(rule.getId(), USER_ID))
                .thenReturn(Optional.of(rule));
        when(transactionReadRepository.findByUserIdAndCategorizationRuleId(USER_ID, rule.getId(), 100, 0))
                .thenReturn(List.of(tracked));

        List<TransactionDto> result = transactionService.getTransactionsForCategorizationRule(USER_ID, rule.getId(), 100, 0);

        assertEquals(1, result.size());
        assertEquals(tracked.getId(), result.get(0).getId());
        verify(transactionReadRepository).findByUserIdAndCategorizationRuleId(USER_ID, rule.getId(), 100, 0);
    }

    @Test
    void getTransactionsForwardsUncategorizedFilter() {
        when(transactionReadRepository.findByUserIdWithFilters(
                USER_ID, false, null, null, null, null, true, null, 100, 0, false))
                .thenReturn(List.of(baseTransaction()));

        List<TransactionDto> result = transactionService.getTransactions(
                USER_ID, false, null, null, null, null, true, null, 100, 0, "desc");

        assertEquals(1, result.size());
        verify(transactionReadRepository).findByUserIdWithFilters(
                USER_ID, false, null, null, null, null, true, null, 100, 0, false);
    }

    @Test
    void getTransactionsForwardsDescriptionQueryFilter() {
        when(transactionReadRepository.findByUserIdWithFilters(
                USER_ID, false, null, null, "Coffee #123!", null, false, null, 100, 0, false))
                .thenReturn(List.of(baseTransaction()));

        List<TransactionDto> result = transactionService.getTransactions(
                USER_ID, false, null, null, "Coffee #123!", null, false, null, 100, 0, "desc");

        assertEquals(1, result.size());
        verify(transactionReadRepository).findByUserIdWithFilters(
                USER_ID, false, null, null, "Coffee #123!", null, false, null, 100, 0, false);
    }

    @Test
    void getTransactionsRejectsCategoryIdWithUncategorizedFilter() {
        ApiException exception = assertThrows(
                ApiException.class,
                () -> transactionService.getTransactions(USER_ID, false, null, null, null, 12L, true, null, 100, 0, "desc")
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        verify(transactionReadRepository, never()).findByUserIdWithFilters(
                eq(USER_ID), eq(false), eq(null), eq(null), eq(null), eq(12L), eq(true), eq(null), eq(100), eq(0), eq(false));
    }

    // --- getTransaction tests ---

    @Test
    void getTransactionReturnsTransactionWhenFound() {
        Transaction tx = baseTransaction();
        when(transactionReadRepository.findByIdAndUserId(TRANSACTION_ID, USER_ID))
                .thenReturn(Optional.of(tx));

        TransactionDto result = transactionService.getTransaction(USER_ID, TRANSACTION_ID);

        assertNotNull(result);
        assertEquals(TRANSACTION_ID, result.getId());
        assertEquals(ACCOUNT_ID, result.getAccountId());
    }

    @Test
    void getTransactionThrowsNotFoundWhenMissing() {
        when(transactionReadRepository.findByIdAndUserId(999L, USER_ID))
                .thenReturn(Optional.empty());

        ApiException exception = assertThrows(
                ApiException.class,
                () -> transactionService.getTransaction(USER_ID, 999L)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    // --- getTransactionCoverage tests ---

    @Test
    void getTransactionCoverageReturnsCoverageStats() {
        Instant oldest = Instant.parse("2025-01-01T00:00:00Z");
        Instant newest = Instant.parse("2026-02-01T00:00:00Z");
        when(transactionReadRepository.getCoverageByUserId(USER_ID))
                .thenReturn(new com.peter.budget.repository.TransactionReadRepository.TransactionCoverageStats(150L, oldest, newest));

        var result = transactionService.getTransactionCoverage(USER_ID);

        assertEquals(150L, result.getTotalTransactions());
        assertEquals(oldest, result.getOldestPostedAt());
        assertEquals(newest, result.getNewestPostedAt());
    }

    // --- updateTransaction with notes and excludeFromTotals ---

    @Test
    void updateTransactionUpdatesNotes() {
        Transaction existing = baseTransaction();
        when(transactionReadRepository.findByIdAndUserId(TRANSACTION_ID, USER_ID))
                .thenReturn(Optional.of(existing));

        TransactionUpdateRequest request = new TransactionUpdateRequest();
        request.setNotes("My important note");

        transactionService.updateTransaction(USER_ID, TRANSACTION_ID, request);

        verify(transactionWriteRepository).save(transactionCaptor.capture());
        assertEquals("My important note", transactionCaptor.getValue().getNotes());
    }

    @Test
    void updateTransactionUpdatesExcludeFromTotals() {
        Transaction existing = baseTransaction();
        when(transactionReadRepository.findByIdAndUserId(TRANSACTION_ID, USER_ID))
                .thenReturn(Optional.of(existing));

        TransactionUpdateRequest request = new TransactionUpdateRequest();
        request.setExcludeFromTotals(true);

        transactionService.updateTransaction(USER_ID, TRANSACTION_ID, request);

        verify(transactionWriteRepository).save(transactionCaptor.capture());
        assertTrue(transactionCaptor.getValue().isExcludeFromTotals());
    }

    @Test
    void updateTransactionThrowsNotFoundForMissingTransaction() {
        when(transactionReadRepository.findByIdAndUserId(999L, USER_ID))
                .thenReturn(Optional.empty());

        TransactionUpdateRequest request = new TransactionUpdateRequest();
        request.setNotes("test");

        ApiException exception = assertThrows(
                ApiException.class,
                () -> transactionService.updateTransaction(USER_ID, 999L, request)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    void updateTransactionThrowsNotFoundForInvalidCategory() {
        Transaction existing = baseTransaction();
        when(transactionReadRepository.findByIdAndUserId(TRANSACTION_ID, USER_ID))
                .thenReturn(Optional.of(existing));
        when(categoryViewService.getEffectiveCategoryByIdForUser(USER_ID, 999L))
                .thenReturn(Optional.empty());

        TransactionUpdateRequest request = new TransactionUpdateRequest();
        request.setCategoryId(999L);

        ApiException exception = assertThrows(
                ApiException.class,
                () -> transactionService.updateTransaction(USER_ID, TRANSACTION_ID, request)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    void deleteTransactionDeletesManualEntry() {
        Transaction manual = baseTransaction();
        manual.setExternalId(null);

        when(transactionReadRepository.findByIdAndUserId(TRANSACTION_ID, USER_ID))
                .thenReturn(Optional.of(manual));

        transactionService.deleteTransaction(USER_ID, TRANSACTION_ID);

        verify(transactionWriteRepository).deleteById(TRANSACTION_ID);
        verify(transferDetectionService, never()).unlinkTransfer(any(Long.class), any(Long.class));
    }

    @Test
    void deleteTransactionRejectsImportedEntry() {
        Transaction imported = baseTransaction();
        imported.setExternalId("sf_123");

        when(transactionReadRepository.findByIdAndUserId(TRANSACTION_ID, USER_ID))
                .thenReturn(Optional.of(imported));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> transactionService.deleteTransaction(USER_ID, TRANSACTION_ID)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        verify(transactionWriteRepository, never()).deleteById(any(Long.class));
    }

    @Test
    void deleteTransactionUnlinksTransferPairBeforeDelete() {
        Transaction manualTransfer = baseTransaction();
        manualTransfer.setTransferPairId(222L);

        when(transactionReadRepository.findByIdAndUserId(TRANSACTION_ID, USER_ID))
                .thenReturn(Optional.of(manualTransfer));

        transactionService.deleteTransaction(USER_ID, TRANSACTION_ID);

        verify(transferDetectionService).unlinkTransfer(USER_ID, TRANSACTION_ID);
        verify(transactionWriteRepository).deleteById(TRANSACTION_ID);
    }

    // --- Delegation methods ---

    @Test
    void getTransfersDelegatesToTransferDetectionService() {
        when(transferDetectionService.getTransferPairs(USER_ID)).thenReturn(List.of());

        transactionService.getTransfers(USER_ID);

        verify(transferDetectionService).getTransferPairs(USER_ID);
    }

    @Test
    void markAsTransferDelegatesToTransferDetectionService() {
        transactionService.markAsTransfer(USER_ID, 1L, 2L);

        verify(transferDetectionService).markAsTransfer(USER_ID, 1L, 2L);
    }

    @Test
    void unlinkTransferDelegatesToTransferDetectionService() {
        transactionService.unlinkTransfer(USER_ID, 1L);

        verify(transferDetectionService).unlinkTransfer(USER_ID, 1L);
    }

    // --- getTransactionsForCategorizationRule edge case ---

    @Test
    void getTransactionsForCategorizationRuleThrowsNotFoundForMissingRule() {
        when(categorizationRuleRepository.findByIdAndUserId(999L, USER_ID))
                .thenReturn(Optional.empty());

        ApiException exception = assertThrows(
                ApiException.class,
                () -> transactionService.getTransactionsForCategorizationRule(USER_ID, 999L, 100, 0)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    // --- Transfer pair account name in toDto ---

    @Test
    void getDtoIncludesTransferPairAccountName() {
        Transaction tx = baseTransaction();
        tx.setTransferPairId(200L);

        Transaction pairTx = Transaction.builder()
                .id(200L).accountId(50L).build();

        Account pairAccount = Account.builder().id(50L).name("Savings").build();

        when(transactionReadRepository.findByIdAndUserId(TRANSACTION_ID, USER_ID))
                .thenReturn(Optional.of(tx));
        when(transactionReadRepository.findById(200L)).thenReturn(Optional.of(pairTx));
        when(accountRepository.findById(50L)).thenReturn(Optional.of(pairAccount));

        TransactionDto result = transactionService.getTransaction(USER_ID, TRANSACTION_ID);

        assertEquals("Savings", result.getTransferPairAccountName());
        assertEquals(200L, result.getTransferPairId());
    }

    @Test
    void backfillCategorizationRulesSetsRuleTrackingForMatchingTransactions() {
        Transaction matching = baseTransaction();
        matching.setId(500L);
        matching.setDescription("TIM HORTONS #2387 TORONTO");
        matching.setCategoryId(null);
        matching.setCategorizedByRuleId(null);
        matching.setManuallyCategorized(false);

        Transaction manual = baseTransaction();
        manual.setId(501L);
        manual.setDescription("Manual category");
        manual.setCategoryId(33L);
        manual.setManuallyCategorized(true);

        when(transactionReadRepository.findByUserId(USER_ID)).thenReturn(List.of(matching, manual));
        when(autoCategorizationService.categorize(
                USER_ID,
                matching.getAccountId(),
                matching.getAmount(),
                matching.getDescription(),
                matching.getPayee(),
                matching.getMemo()
        ))
                .thenReturn(new AutoCategorizationService.CategorizationMatch(39L, 24L));

        var result = transactionService.backfillCategorizationRules(USER_ID);

        assertEquals(2, result.getTotalTransactions());
        assertEquals(1, result.getEligibleTransactions());
        assertEquals(1, result.getMatchedTransactions());
        assertEquals(1, result.getUpdatedTransactions());

        verify(transactionWriteRepository).save(transactionCaptor.capture());
        Transaction saved = transactionCaptor.getValue();
        assertEquals(24L, saved.getCategoryId());
        assertEquals(39L, saved.getCategorizedByRuleId());
        assertFalse(saved.isManuallyCategorized());
    }

    @Test
    void backfillCategorizationRulesClearsCategoryWhenRuleNoLongerMatches() {
        Transaction staleAutoCategorized = baseTransaction();
        staleAutoCategorized.setId(700L);
        staleAutoCategorized.setDescription("Prime Member payment");
        staleAutoCategorized.setCategoryId(24L);
        staleAutoCategorized.setCategorizedByRuleId(39L);
        staleAutoCategorized.setManuallyCategorized(false);

        when(transactionReadRepository.findByUserId(USER_ID)).thenReturn(List.of(staleAutoCategorized));
        when(autoCategorizationService.categorize(
                USER_ID,
                staleAutoCategorized.getAccountId(),
                staleAutoCategorized.getAmount(),
                staleAutoCategorized.getDescription(),
                staleAutoCategorized.getPayee(),
                staleAutoCategorized.getMemo()
        ))
                .thenReturn(null);

        var result = transactionService.backfillCategorizationRules(USER_ID);

        assertEquals(1, result.getTotalTransactions());
        assertEquals(1, result.getEligibleTransactions());
        assertEquals(0, result.getMatchedTransactions());
        assertEquals(1, result.getUpdatedTransactions());

        verify(transactionWriteRepository).save(transactionCaptor.capture());
        Transaction saved = transactionCaptor.getValue();
        assertNull(saved.getCategoryId());
        assertNull(saved.getCategorizedByRuleId());
        assertFalse(saved.isManuallyCategorized());
    }

    private Transaction baseTransaction() {
        return Transaction.builder()
                .id(TRANSACTION_ID)
                .accountId(ACCOUNT_ID)
                .postedAt(Instant.parse("2026-02-01T00:00:00Z"))
                .amount(java.math.BigDecimal.TEN.negate())
                .pending(false)
                .description("Coffee")
                .manuallyCategorized(false)
                .internalTransfer(false)
                .excludeFromTotals(false)
                .recurring(false)
                .build();
    }
}
