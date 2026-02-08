package com.peter.budget.service;

import com.peter.budget.exception.ApiException;
import com.peter.budget.model.dto.AccountDto;
import com.peter.budget.model.dto.AccountNetWorthCategoryUpdateRequest;
import com.peter.budget.model.dto.AccountSummaryDto;
import com.peter.budget.model.entity.Account;
import com.peter.budget.model.enums.AccountNetWorthCategory;
import com.peter.budget.model.enums.AccountType;
import com.peter.budget.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;

    public List<AccountDto> getAccounts(Long userId) {
        return accountRepository.findActiveByUserId(userId).stream()
                .map(this::toDto)
                .toList();
    }

    public AccountDto getAccount(Long userId, Long accountId) {
        Account account = accountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> ApiException.notFound("Account not found"));
        return toDto(account);
    }

    public AccountDto updateNetWorthCategory(Long userId, Long accountId, AccountNetWorthCategoryUpdateRequest request) {
        Account account = accountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> ApiException.notFound("Account not found"));
        account.setNetWorthCategoryOverride(request.getNetWorthCategory());
        Account updated = accountRepository.save(account);
        return toDto(updated);
    }

    public AccountSummaryDto getAccountSummary(Long userId) {
        List<Account> accounts = accountRepository.findActiveByUserId(userId);
        BigDecimal totalAssets = BigDecimal.ZERO;
        BigDecimal totalLiabilities = BigDecimal.ZERO;

        for (Account account : accounts) {
            BigDecimal balance = account.getCurrentBalance() != null ? account.getCurrentBalance() : BigDecimal.ZERO;
            AccountNetWorthCategory category = resolveNetWorthCategory(account, balance);

            if (category == AccountNetWorthCategory.LIABILITY) {
                totalLiabilities = totalLiabilities.add(balance.abs());
                continue;
            }

            if (category == AccountNetWorthCategory.BANK_ACCOUNT || category == AccountNetWorthCategory.INVESTMENT) {
                totalAssets = totalAssets.add(balance);
                continue;
            }
        }

        BigDecimal netWorth = totalAssets.subtract(totalLiabilities);

        List<AccountDto> accountDtos = accounts.stream()
                .map(this::toDto)
                .toList();

        return AccountSummaryDto.builder()
                .totalAssets(totalAssets)
                .totalLiabilities(totalLiabilities)
                .netWorth(netWorth)
                .accounts(accountDtos)
                .build();
    }

    private boolean isLiabilityType(AccountType accountType) {
        return accountType == AccountType.CREDIT_CARD
                || accountType == AccountType.LOAN;
    }

    private AccountDto toDto(Account account) {
        return AccountDto.builder()
                .id(account.getId())
                .name(account.getName())
                .institutionName(account.getInstitutionName())
                .accountType(account.getAccountType())
                .netWorthCategory(resolveNetWorthCategory(account, account.getCurrentBalance()))
                .currency(account.getCurrency())
                .currentBalance(account.getCurrentBalance())
                .availableBalance(account.getAvailableBalance())
                .balanceUpdatedAt(account.getBalanceUpdatedAt())
                .active(account.isActive())
                .build();
    }

    private AccountNetWorthCategory resolveNetWorthCategory(Account account, BigDecimal maybeBalance) {
        if (account.getNetWorthCategoryOverride() != null) {
            return account.getNetWorthCategoryOverride();
        }

        if (account.getAccountType() == AccountType.CHECKING || account.getAccountType() == AccountType.SAVINGS) {
            return AccountNetWorthCategory.BANK_ACCOUNT;
        }

        if (account.getAccountType() == AccountType.INVESTMENT) {
            return AccountNetWorthCategory.INVESTMENT;
        }

        if (isLiabilityType(account.getAccountType())) {
            return AccountNetWorthCategory.LIABILITY;
        }

        BigDecimal balance = maybeBalance != null ? maybeBalance : BigDecimal.ZERO;
        return balance.signum() < 0
                ? AccountNetWorthCategory.LIABILITY
                : AccountNetWorthCategory.BANK_ACCOUNT;
    }
}
