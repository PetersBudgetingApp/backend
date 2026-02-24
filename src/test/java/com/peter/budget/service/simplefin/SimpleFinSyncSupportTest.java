package com.peter.budget.service.simplefin;

import com.peter.budget.model.entity.Account;
import com.peter.budget.model.entity.Transaction;
import com.peter.budget.model.enums.AccountType;
import com.peter.budget.repository.AccountRepository;
import com.peter.budget.repository.TransactionReadRepository;
import com.peter.budget.repository.TransactionWriteRepository;
import com.peter.budget.service.AutoCategorizationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SimpleFinSyncSupportTest {

    private static final long USER_ID = 7L;
    private static final long CONNECTION_ID = 10L;
    private static final long ACCOUNT_ID = 100L;

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private TransactionReadRepository transactionReadRepository;
    @Mock
    private TransactionWriteRepository transactionWriteRepository;
    @Mock
    private AutoCategorizationService categorizationService;

    @InjectMocks
    private SimpleFinSyncSupport syncSupport;

    @Captor
    private ArgumentCaptor<Account> accountCaptor;
    @Captor
    private ArgumentCaptor<Transaction> transactionCaptor;

    // --- summarizeInstitutionNames tests ---

    @Test
    void summarizeInstitutionNamesReturnsSingleName() {
        String result = syncSupport.summarizeInstitutionNames(List.of("Chase Bank"), null);
        assertEquals("Chase Bank", result);
    }

    @Test
    void summarizeInstitutionNamesReturnsCombinedForMultiple() {
        String result = syncSupport.summarizeInstitutionNames(
                List.of("Chase Bank", "Wells Fargo", "Bank of America"), null);
        assertEquals("Chase Bank + 2 others", result);
    }

    @Test
    void summarizeInstitutionNamesReturnsSingularOtherForTwo() {
        String result = syncSupport.summarizeInstitutionNames(
                List.of("Chase Bank", "Wells Fargo"), null);
        assertEquals("Chase Bank + 1 other", result);
    }

    @Test
    void summarizeInstitutionNamesReturnsUnknownWhenEmpty() {
        String result = syncSupport.summarizeInstitutionNames(List.of(), null);
        assertEquals("Unknown institution", result);
    }

    @Test
    void summarizeInstitutionNamesReturnsFallbackWhenEmptyWithFallback() {
        String result = syncSupport.summarizeInstitutionNames(List.of(), "My Bank");
        assertEquals("My Bank", result);
    }

    @Test
    void summarizeInstitutionNamesFiltersNullAndBlank() {
        String result = syncSupport.summarizeInstitutionNames(
                List.of("Chase Bank", "", "  ", "Chase Bank"), null);
        assertEquals("Chase Bank", result); // deduped
    }

    @Test
    void summarizeInstitutionNamesDeduplicates() {
        String result = syncSupport.summarizeInstitutionNames(
                List.of("Chase", "Chase", "Chase"), null);
        assertEquals("Chase", result);
    }

    // --- createOrUpdateAccount tests ---

    @Test
    void createOrUpdateAccountCreatesNewAccount() {
        SimpleFinClient.SimpleFinAccount sfAccount = new SimpleFinClient.SimpleFinAccount(
                "ext-123", "My Checking", "Chase", "USD",
                new BigDecimal("1234.56"), new BigDecimal("1000.00"),
                null, "CHECKING", List.of()
        );

        when(accountRepository.findByConnectionIdAndExternalId(CONNECTION_ID, "ext-123"))
                .thenReturn(Optional.empty());
        when(accountRepository.save(any(Account.class))).thenAnswer(i -> {
            Account a = i.getArgument(0);
            a.setId(ACCOUNT_ID);
            return a;
        });

        Account result = syncSupport.createOrUpdateAccount(USER_ID, CONNECTION_ID, sfAccount);

        assertNotNull(result);
        verify(accountRepository).save(accountCaptor.capture());
        Account saved = accountCaptor.getValue();
        assertEquals(USER_ID, saved.getUserId());
        assertEquals(CONNECTION_ID, saved.getConnectionId());
        assertEquals("ext-123", saved.getExternalId());
        assertEquals("My Checking", saved.getName());
        assertEquals(AccountType.CHECKING, saved.getAccountType());
        assertEquals("USD", saved.getCurrency());
        assertEquals(new BigDecimal("1234.56"), saved.getCurrentBalance());
    }

    @Test
    void createOrUpdateAccountUpdatesExistingAccount() {
        Account existing = Account.builder()
                .id(ACCOUNT_ID)
                .userId(USER_ID)
                .connectionId(CONNECTION_ID)
                .externalId("ext-123")
                .name("Old Name")
                .accountType(AccountType.CHECKING)
                .currentBalance(new BigDecimal("500.00"))
                .active(true)
                .build();

        SimpleFinClient.SimpleFinAccount sfAccount = new SimpleFinClient.SimpleFinAccount(
                "ext-123", "New Name", "Chase", "USD",
                new BigDecimal("750.00"), null, null, "CHECKING", List.of()
        );

        when(accountRepository.findByConnectionIdAndExternalId(CONNECTION_ID, "ext-123"))
                .thenReturn(Optional.of(existing));
        when(accountRepository.save(any(Account.class))).thenAnswer(i -> i.getArgument(0));

        Account result = syncSupport.createOrUpdateAccount(USER_ID, CONNECTION_ID, sfAccount);

        assertEquals("New Name", result.getName());
        assertEquals(new BigDecimal("750.00"), result.getCurrentBalance());
    }

    @Test
    void createOrUpdateAccountDefaultsCurrencyToUSD() {
        SimpleFinClient.SimpleFinAccount sfAccount = new SimpleFinClient.SimpleFinAccount(
                "ext-123", "Account", "Bank", null,
                BigDecimal.ZERO, null, null, "CHECKING", List.of()
        );

        when(accountRepository.findByConnectionIdAndExternalId(CONNECTION_ID, "ext-123"))
                .thenReturn(Optional.empty());
        when(accountRepository.save(any(Account.class))).thenAnswer(i -> i.getArgument(0));

        syncSupport.createOrUpdateAccount(USER_ID, CONNECTION_ID, sfAccount);

        verify(accountRepository).save(accountCaptor.capture());
        assertEquals("USD", accountCaptor.getValue().getCurrency());
    }

    @Test
    void createOrUpdateAccountUsesProviderBalanceDateWhenPresent() {
        Instant providerBalanceDate = Instant.parse("2026-01-20T12:00:00Z");
        SimpleFinClient.SimpleFinAccount sfAccount = new SimpleFinClient.SimpleFinAccount(
                "ext-123", "My Checking", "Chase", "USD",
                new BigDecimal("1234.56"), new BigDecimal("1000.00"),
                providerBalanceDate, "CHECKING", List.of()
        );

        when(accountRepository.findByConnectionIdAndExternalId(CONNECTION_ID, "ext-123"))
                .thenReturn(Optional.empty());
        when(accountRepository.save(any(Account.class))).thenAnswer(i -> i.getArgument(0));

        syncSupport.createOrUpdateAccount(USER_ID, CONNECTION_ID, sfAccount);

        verify(accountRepository).save(accountCaptor.capture());
        assertEquals(providerBalanceDate, accountCaptor.getValue().getBalanceUpdatedAt());
    }

    @Test
    void createOrUpdateAccountKeepsExistingBalanceDateWhenProviderOmitsIt() {
        Instant existingBalanceDate = Instant.parse("2025-12-15T00:00:00Z");
        Account existing = Account.builder()
                .id(ACCOUNT_ID)
                .userId(USER_ID)
                .connectionId(CONNECTION_ID)
                .externalId("ext-123")
                .name("Old Name")
                .accountType(AccountType.CHECKING)
                .balanceUpdatedAt(existingBalanceDate)
                .active(true)
                .build();

        SimpleFinClient.SimpleFinAccount sfAccount = new SimpleFinClient.SimpleFinAccount(
                "ext-123", "Updated Name", "Chase", "USD",
                new BigDecimal("750.00"), null, null, "CHECKING", List.of()
        );

        when(accountRepository.findByConnectionIdAndExternalId(CONNECTION_ID, "ext-123"))
                .thenReturn(Optional.of(existing));
        when(accountRepository.save(any(Account.class))).thenAnswer(i -> i.getArgument(0));

        syncSupport.createOrUpdateAccount(USER_ID, CONNECTION_ID, sfAccount);

        verify(accountRepository).save(accountCaptor.capture());
        assertEquals(existingBalanceDate, accountCaptor.getValue().getBalanceUpdatedAt());
    }

    // --- syncTransactions tests ---

    @Test
    void syncTransactionsCreatesNewTransactions() {
        Account account = Account.builder()
                .id(ACCOUNT_ID).userId(USER_ID).build();

        SimpleFinClient.SimpleFinTransaction sfTx = new SimpleFinClient.SimpleFinTransaction(
                "tx-001", Instant.parse("2026-01-15T00:00:00Z"), null,
                new BigDecimal("-25.00"), false, "Coffee Shop", null, null
        );

        when(transactionReadRepository.findByAccountIdAndExternalId(ACCOUNT_ID, "tx-001"))
                .thenReturn(Optional.empty());
        when(categorizationService.categorize(eq(USER_ID), eq(ACCOUNT_ID), any(), any(), any(), any()))
                .thenReturn(new AutoCategorizationService.CategorizationMatch(42L, 10L));

        SimpleFinSyncSupport.SyncTransactionResult result =
                syncSupport.syncTransactions(account, List.of(sfTx));

        assertEquals(1, result.added());
        assertEquals(0, result.updated());
        verify(transactionWriteRepository).save(transactionCaptor.capture());
        Transaction saved = transactionCaptor.getValue();
        assertEquals("tx-001", saved.getExternalId());
        assertEquals(new BigDecimal("-25.00"), saved.getAmount());
        assertEquals(10L, saved.getCategoryId());
        assertEquals(42L, saved.getCategorizedByRuleId());
    }

    @Test
    void syncTransactionsUpdatesExistingTransactionWhenChanged() {
        Account account = Account.builder()
                .id(ACCOUNT_ID).userId(USER_ID).build();

        Transaction existing = Transaction.builder()
                .id(500L)
                .accountId(ACCOUNT_ID)
                .externalId("tx-001")
                .amount(new BigDecimal("-25.00"))
                .pending(true) // was pending
                .manuallyCategorized(false)
                .build();

        SimpleFinClient.SimpleFinTransaction sfTx = new SimpleFinClient.SimpleFinTransaction(
                "tx-001", Instant.parse("2026-01-15T00:00:00Z"), null,
                new BigDecimal("-25.00"), false, "Coffee Shop", null, null // no longer pending
        );

        when(transactionReadRepository.findByAccountIdAndExternalId(ACCOUNT_ID, "tx-001"))
                .thenReturn(Optional.of(existing));

        SimpleFinSyncSupport.SyncTransactionResult result =
                syncSupport.syncTransactions(account, List.of(sfTx));

        assertEquals(0, result.added());
        assertEquals(1, result.updated());
        verify(transactionWriteRepository).save(transactionCaptor.capture());
        assertFalse(transactionCaptor.getValue().isPending());
    }

    @Test
    void syncTransactionsDoesNotUpdateUnchangedTransaction() {
        Account account = Account.builder()
                .id(ACCOUNT_ID).userId(USER_ID).build();

        Transaction existing = Transaction.builder()
                .id(500L)
                .accountId(ACCOUNT_ID)
                .externalId("tx-001")
                .amount(new BigDecimal("-25.00"))
                .pending(false)
                .manuallyCategorized(true) // manually categorized
                .categoryId(10L)
                .build();

        SimpleFinClient.SimpleFinTransaction sfTx = new SimpleFinClient.SimpleFinTransaction(
                "tx-001", Instant.parse("2026-01-15T00:00:00Z"), null,
                new BigDecimal("-25.00"), false, "Coffee Shop", null, null
        );

        when(transactionReadRepository.findByAccountIdAndExternalId(ACCOUNT_ID, "tx-001"))
                .thenReturn(Optional.of(existing));

        SimpleFinSyncSupport.SyncTransactionResult result =
                syncSupport.syncTransactions(account, List.of(sfTx));

        assertEquals(0, result.added());
        assertEquals(0, result.updated());
        verify(transactionWriteRepository, never()).save(any(Transaction.class));
    }

    @Test
    void syncTransactionsAutoCategorizesDuringUpdate() {
        Account account = Account.builder()
                .id(ACCOUNT_ID).userId(USER_ID).build();

        Transaction existing = Transaction.builder()
                .id(500L)
                .accountId(ACCOUNT_ID)
                .externalId("tx-001")
                .amount(new BigDecimal("-25.00"))
                .pending(true)
                .manuallyCategorized(false)
                .categoryId(null)
                .build();

        SimpleFinClient.SimpleFinTransaction sfTx = new SimpleFinClient.SimpleFinTransaction(
                "tx-001", Instant.parse("2026-01-15T00:00:00Z"), null,
                new BigDecimal("-25.00"), false, "Coffee Shop", null, null
        );

        when(transactionReadRepository.findByAccountIdAndExternalId(ACCOUNT_ID, "tx-001"))
                .thenReturn(Optional.of(existing));
        when(categorizationService.categorize(eq(USER_ID), eq(ACCOUNT_ID), any(), any(), any(), any()))
                .thenReturn(new AutoCategorizationService.CategorizationMatch(42L, 10L));

        SimpleFinSyncSupport.SyncTransactionResult result =
                syncSupport.syncTransactions(account, List.of(sfTx));

        assertEquals(0, result.added());
        assertEquals(1, result.updated());
        verify(transactionWriteRepository).save(transactionCaptor.capture());
        assertEquals(10L, transactionCaptor.getValue().getCategoryId());
        assertEquals(42L, transactionCaptor.getValue().getCategorizedByRuleId());
    }

    @Test
    void syncTransactionsCreatesWithoutAutoCategorizationWhenNoMatch() {
        Account account = Account.builder()
                .id(ACCOUNT_ID).userId(USER_ID).build();

        SimpleFinClient.SimpleFinTransaction sfTx = new SimpleFinClient.SimpleFinTransaction(
                "tx-002", Instant.parse("2026-01-15T00:00:00Z"), null,
                new BigDecimal("-10.00"), false, "Random Store", null, null
        );

        when(transactionReadRepository.findByAccountIdAndExternalId(ACCOUNT_ID, "tx-002"))
                .thenReturn(Optional.empty());
        when(categorizationService.categorize(eq(USER_ID), eq(ACCOUNT_ID), any(), any(), any(), any()))
                .thenReturn(null);

        syncSupport.syncTransactions(account, List.of(sfTx));

        verify(transactionWriteRepository).save(transactionCaptor.capture());
        assertNull(transactionCaptor.getValue().getCategoryId());
        assertNull(transactionCaptor.getValue().getCategorizedByRuleId());
    }

    @Test
    void syncTransactionsClearsStaleRuleTrackingOnUpdate() {
        Account account = Account.builder()
                .id(ACCOUNT_ID).userId(USER_ID).build();

        Transaction existing = Transaction.builder()
                .id(500L)
                .accountId(ACCOUNT_ID)
                .externalId("tx-001")
                .amount(new BigDecimal("-25.00"))
                .pending(false)
                .manuallyCategorized(false)
                .categoryId(null)
                .categorizedByRuleId(99L) // was previously matched
                .build();

        SimpleFinClient.SimpleFinTransaction sfTx = new SimpleFinClient.SimpleFinTransaction(
                "tx-001", Instant.parse("2026-01-15T00:00:00Z"), null,
                new BigDecimal("-25.00"), false, "Coffee Shop", null, null
        );

        when(transactionReadRepository.findByAccountIdAndExternalId(ACCOUNT_ID, "tx-001"))
                .thenReturn(Optional.of(existing));
        when(categorizationService.categorize(eq(USER_ID), eq(ACCOUNT_ID), any(), any(), any(), any()))
                .thenReturn(null); // no longer matches

        syncSupport.syncTransactions(account, List.of(sfTx));

        verify(transactionWriteRepository).save(transactionCaptor.capture());
        assertNull(transactionCaptor.getValue().getCategorizedByRuleId());
    }
}
