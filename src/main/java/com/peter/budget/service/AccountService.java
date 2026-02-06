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

        BigDecimal totalAssets = accountRepository.sumBalancesByUserIdAndAccountTypes(
                userId, List.of(AccountType.CHECKING, AccountType.SAVINGS, AccountType.INVESTMENT));

        BigDecimal totalLiabilities = accountRepository.sumBalancesByUserIdAndAccountTypes(
                userId, List.of(AccountType.CREDIT_CARD, AccountType.LOAN))
                .abs();

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
