package com.peter.budget.service;

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

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
        when(autoCategorizationService.categorize(USER_ID, matching.getDescription(), matching.getPayee(), matching.getMemo()))
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
