package com.peter.budget.service;

import com.peter.budget.exception.ApiException;
import com.peter.budget.model.dto.AccountDto;
import com.peter.budget.model.dto.AccountSummaryDto;
import com.peter.budget.model.entity.Account;
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

    public AccountSummaryDto getAccountSummary(Long userId) {
        List<Account> accounts = accountRepository.findActiveByUserId(userId);
        BigDecimal totalAssets = BigDecimal.ZERO;
        BigDecimal totalLiabilities = BigDecimal.ZERO;

        for (Account account : accounts) {
            BigDecimal balance = account.getCurrentBalance() != null ? account.getCurrentBalance() : BigDecimal.ZERO;

            if (isLiabilityType(account.getAccountType())) {
                totalLiabilities = totalLiabilities.add(balance.abs());
                continue;
            }

            if (isAssetType(account.getAccountType())) {
                totalAssets = totalAssets.add(balance);
                continue;
            }

            // Fallback for unmapped/unknown account types.
            if (balance.signum() >= 0) {
                totalAssets = totalAssets.add(balance);
            } else {
                totalLiabilities = totalLiabilities.add(balance.abs());
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

    private boolean isAssetType(AccountType accountType) {
        return accountType == AccountType.CHECKING
                || accountType == AccountType.SAVINGS
                || accountType == AccountType.INVESTMENT;
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
                .currency(account.getCurrency())
                .currentBalance(account.getCurrentBalance())
                .availableBalance(account.getAvailableBalance())
                .balanceUpdatedAt(account.getBalanceUpdatedAt())
                .active(account.isActive())
                .build();
    }
}
