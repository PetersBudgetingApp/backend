package com.peter.budget.service;

import com.peter.budget.model.dto.AccountSummaryDto;
import com.peter.budget.model.entity.Account;
import com.peter.budget.model.enums.AccountType;
import com.peter.budget.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    private static final long USER_ID = 42L;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AccountService accountService;

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
