package com.peter.budget.service;

import com.peter.budget.exception.ApiException;
import com.peter.budget.model.dto.AccountCreateRequest;
import com.peter.budget.model.dto.AccountDeletionPreviewDto;
import com.peter.budget.model.dto.AccountDto;
import com.peter.budget.model.dto.AccountNetWorthCategoryUpdateRequest;
import com.peter.budget.model.dto.AccountSummaryDto;
import com.peter.budget.model.entity.Account;
import com.peter.budget.model.enums.AccountNetWorthCategory;
import com.peter.budget.model.enums.AccountType;
import com.peter.budget.repository.AccountRepository;
import com.peter.budget.repository.TransactionReadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final TransactionReadRepository transactionReadRepository;

    public List<AccountDto> getAccounts(Long userId) {
        return accountRepository.findActiveByUserId(userId).stream()
                .map(this::toDto)
                .toList();
    }

    public AccountDto createAccount(Long userId, AccountCreateRequest request) {
        String name = request.getName() != null ? request.getName().trim() : "";
        if (name.isEmpty()) {
            throw ApiException.badRequest("Name is required");
        }

        String institutionName = request.getInstitutionName() != null
                ? request.getInstitutionName().trim()
                : "";
        if (institutionName.isEmpty()) {
            institutionName = "User Added";
        }

        String requestedCurrency = request.getCurrency() != null
                ? request.getCurrency().trim().toUpperCase(Locale.ROOT)
                : "";
        String currency = requestedCurrency.isEmpty()
                ? accountRepository.findActiveByUserId(userId).stream()
                    .findFirst()
                    .map(Account::getCurrency)
                    .orElse("USD")
                : requestedCurrency;

        Instant now = Instant.now();
        Account account = Account.builder()
                .userId(userId)
                .connectionId(null)
                .externalId(null)
                .name(name)
                .institutionName(institutionName)
                .accountType(accountTypeForCategory(request.getNetWorthCategory()))
                .netWorthCategoryOverride(request.getNetWorthCategory())
                .currency(currency)
                .currentBalance(request.getCurrentBalance())
                .availableBalance(request.getCurrentBalance())
                .balanceUpdatedAt(now)
                .active(true)
                .build();

        Account created = accountRepository.save(account);
        return toDto(created);
    }

    public AccountDto getAccount(Long userId, Long accountId) {
        Account account = accountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> ApiException.notFound("Account not found"));
        return toDto(account);
    }

    public AccountDeletionPreviewDto getDeletionPreview(Long userId, Long accountId) {
        Account account = accountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> ApiException.notFound("Account not found"));

        long transactionCount = transactionReadRepository.countByUserIdAndAccountId(userId, accountId);
        return AccountDeletionPreviewDto.builder()
                .transactionCount(transactionCount)
                .canDelete(isManuallyCreatedAccount(account))
                .build();
    }

    public void deleteAccount(Long userId, Long accountId) {
        Account account = accountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> ApiException.notFound("Account not found"));

        if (!isManuallyCreatedAccount(account)) {
            throw ApiException.badRequest("Only manually created accounts can be deleted");
        }

        accountRepository.deleteByIdAndUserId(accountId, userId);
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

    private AccountType accountTypeForCategory(AccountNetWorthCategory category) {
        return switch (category) {
            case BANK_ACCOUNT -> AccountType.CHECKING;
            case INVESTMENT -> AccountType.INVESTMENT;
            case LIABILITY -> AccountType.LOAN;
        };
    }

    private boolean isManuallyCreatedAccount(Account account) {
        return account.getConnectionId() == null;
    }
}
