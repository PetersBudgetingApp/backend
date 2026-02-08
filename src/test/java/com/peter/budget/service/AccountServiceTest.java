package com.peter.budget.service;

import com.peter.budget.exception.ApiException;
import com.peter.budget.model.dto.AccountDto;
import com.peter.budget.model.dto.AccountNetWorthCategoryUpdateRequest;
import com.peter.budget.model.dto.AccountSummaryDto;
import com.peter.budget.model.entity.Account;
import com.peter.budget.model.enums.AccountNetWorthCategory;
import com.peter.budget.model.enums.AccountType;
import com.peter.budget.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    private static final long USER_ID = 42L;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AccountService accountService;

    // --- getAccounts tests ---

    @Test
    void getAccountsReturnsActiveAccounts() {
        when(accountRepository.findActiveByUserId(USER_ID)).thenReturn(List.of(
                account(1L, AccountType.CHECKING, "500.00"),
                account(2L, AccountType.SAVINGS, "1000.00")
        ));

        List<AccountDto> result = accountService.getAccounts(USER_ID);

        assertEquals(2, result.size());
        assertEquals("Account 1", result.get(0).getName());
        assertEquals(AccountType.CHECKING, result.get(0).getAccountType());
    }

    @Test
    void getAccountsReturnsEmptyListWhenNoAccounts() {
        when(accountRepository.findActiveByUserId(USER_ID)).thenReturn(List.of());

        List<AccountDto> result = accountService.getAccounts(USER_ID);

        assertTrue(result.isEmpty());
    }

    // --- getAccount tests ---

    @Test
    void getAccountReturnsAccountById() {
        Account acct = account(1L, AccountType.CHECKING, "500.00");
        when(accountRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(acct));

        AccountDto result = accountService.getAccount(USER_ID, 1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(AccountType.CHECKING, result.getAccountType());
        assertEquals(AccountNetWorthCategory.BANK_ACCOUNT, result.getNetWorthCategory());
        assertEquals(new BigDecimal("500.00"), result.getCurrentBalance());
    }

    @Test
    void getAccountThrowsNotFoundWhenMissing() {
        when(accountRepository.findByIdAndUserId(999L, USER_ID)).thenReturn(Optional.empty());

        ApiException exception = assertThrows(
                ApiException.class,
                () -> accountService.getAccount(USER_ID, 999L)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertEquals("Account not found", exception.getMessage());
    }

    // --- getAccountSummary tests ---

    @Test
    void getAccountSummaryUsesSignedFallbackForOtherAccountTypes() {
        when(accountRepository.findActiveByUserId(USER_ID)).thenReturn(List.of(
                account(1L, AccountType.OTHER, "120.50"),
                account(2L, AccountType.OTHER, "-20.25"),
                account(3L, AccountType.CHECKING, "100.00"),
                account(4L, AccountType.CREDIT_CARD, "300.00")
        ));

        AccountSummaryDto summary = accountService.getAccountSummary(USER_ID);

        assertEquals(new BigDecimal("220.50"), summary.getTotalAssets());
        assertEquals(new BigDecimal("320.25"), summary.getTotalLiabilities());
        assertEquals(new BigDecimal("-99.75"), summary.getNetWorth());
    }

    @Test
    void getAccountSummaryTreatsNegativeLiabilityBalanceAsLiability() {
        when(accountRepository.findActiveByUserId(USER_ID)).thenReturn(List.of(
                account(10L, AccountType.CREDIT_CARD, "-55.00")
        ));

        AccountSummaryDto summary = accountService.getAccountSummary(USER_ID);

        assertEquals(BigDecimal.ZERO, summary.getTotalAssets());
        assertEquals(new BigDecimal("55.00"), summary.getTotalLiabilities());
        assertEquals(new BigDecimal("-55.00"), summary.getNetWorth());
    }

    @Test
    void getAccountSummaryUsesOverrideCategoryWhenSet() {
        Account account = account(1L, AccountType.OTHER, "250.00");
        account.setNetWorthCategoryOverride(AccountNetWorthCategory.LIABILITY);

        when(accountRepository.findActiveByUserId(USER_ID)).thenReturn(List.of(account));

        AccountSummaryDto summary = accountService.getAccountSummary(USER_ID);

        assertEquals(BigDecimal.ZERO, summary.getTotalAssets());
        assertEquals(new BigDecimal("250.00"), summary.getTotalLiabilities());
        assertEquals(new BigDecimal("-250.00"), summary.getNetWorth());
        assertEquals(AccountNetWorthCategory.LIABILITY, summary.getAccounts().get(0).getNetWorthCategory());
    }

    @Test
    void getAccountSummaryClassifiesAssetTypes() {
        when(accountRepository.findActiveByUserId(USER_ID)).thenReturn(List.of(
                account(1L, AccountType.CHECKING, "1000.00"),
                account(2L, AccountType.SAVINGS, "5000.00"),
                account(3L, AccountType.INVESTMENT, "20000.00")
        ));

        AccountSummaryDto summary = accountService.getAccountSummary(USER_ID);

        assertEquals(new BigDecimal("26000.00"), summary.getTotalAssets());
        assertEquals(BigDecimal.ZERO, summary.getTotalLiabilities());
        assertEquals(new BigDecimal("26000.00"), summary.getNetWorth());
    }

    @Test
    void getAccountSummaryClassifiesLiabilityTypes() {
        when(accountRepository.findActiveByUserId(USER_ID)).thenReturn(List.of(
                account(1L, AccountType.CREDIT_CARD, "2000.00"),
                account(2L, AccountType.LOAN, "15000.00")
        ));

        AccountSummaryDto summary = accountService.getAccountSummary(USER_ID);

        assertEquals(BigDecimal.ZERO, summary.getTotalAssets());
        assertEquals(new BigDecimal("17000.00"), summary.getTotalLiabilities());
        assertEquals(new BigDecimal("-17000.00"), summary.getNetWorth());
    }

    @Test
    void getAccountSummaryHandlesNullBalance() {
        Account acct = Account.builder()
                .id(1L).userId(USER_ID).name("No Balance")
                .accountType(AccountType.CHECKING).currency("USD")
                .currentBalance(null).active(true).build();

        when(accountRepository.findActiveByUserId(USER_ID)).thenReturn(List.of(acct));

        AccountSummaryDto summary = accountService.getAccountSummary(USER_ID);

        assertEquals(BigDecimal.ZERO, summary.getTotalAssets());
        assertEquals(BigDecimal.ZERO, summary.getNetWorth());
    }

    @Test
    void getAccountSummaryReturnsEmptyWhenNoAccounts() {
        when(accountRepository.findActiveByUserId(USER_ID)).thenReturn(List.of());

        AccountSummaryDto summary = accountService.getAccountSummary(USER_ID);

        assertEquals(BigDecimal.ZERO, summary.getTotalAssets());
        assertEquals(BigDecimal.ZERO, summary.getTotalLiabilities());
        assertEquals(BigDecimal.ZERO, summary.getNetWorth());
        assertTrue(summary.getAccounts().isEmpty());
    }

    @Test
    void getAccountSummaryIncludesAccountDtos() {
        when(accountRepository.findActiveByUserId(USER_ID)).thenReturn(List.of(
                account(1L, AccountType.CHECKING, "500.00")
        ));

        AccountSummaryDto summary = accountService.getAccountSummary(USER_ID);

        assertEquals(1, summary.getAccounts().size());
        assertEquals(1L, summary.getAccounts().get(0).getId());
    }

    // --- updateNetWorthCategory tests ---

    @Test
    void updateNetWorthCategoryPersistsOverride() {
        Account account = account(1L, AccountType.CHECKING, "500.00");
        AccountNetWorthCategoryUpdateRequest request = new AccountNetWorthCategoryUpdateRequest();
        request.setNetWorthCategory(AccountNetWorthCategory.LIABILITY);

        when(accountRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AccountDto result = accountService.updateNetWorthCategory(USER_ID, 1L, request);

        assertEquals(AccountNetWorthCategory.LIABILITY, account.getNetWorthCategoryOverride());
        assertEquals(AccountNetWorthCategory.LIABILITY, result.getNetWorthCategory());
    }

    private Account account(Long id, AccountType type, String balance) {
        return Account.builder()
                .id(id)
                .userId(USER_ID)
                .name("Account " + id)
                .accountType(type)
                .currency("USD")
                .currentBalance(new BigDecimal(balance))
                .active(true)
                .build();
    }
}
