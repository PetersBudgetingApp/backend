package com.peter.budget.service.simplefin;

import com.peter.budget.model.entity.Account;
import com.peter.budget.model.entity.Transaction;
import com.peter.budget.model.enums.AccountType;
import com.peter.budget.repository.AccountRepository;
import com.peter.budget.repository.TransactionReadRepository;
import com.peter.budget.repository.TransactionWriteRepository;
import com.peter.budget.service.AutoCategorizationService;
import com.peter.budget.service.UncategorizedCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SimpleFinSyncSupport {

    private final AccountRepository accountRepository;
    private final TransactionReadRepository transactionReadRepository;
    private final TransactionWriteRepository transactionWriteRepository;
    private final AutoCategorizationService categorizationService;
    private final UncategorizedCategoryService uncategorizedCategoryService;

    public String summarizeInstitutionNames(List<String> names, String fallback) {
        Set<String> unique = new LinkedHashSet<>();
        for (String name : names) {
            if (name != null && !name.isBlank()) {
                unique.add(name.trim());
            }
        }

        if (unique.isEmpty()) {
            if (fallback != null && !fallback.isBlank()) {
                return fallback;
            }
            return "Unknown institution";
        }

        if (unique.size() == 1) {
            return unique.iterator().next();
        }

        String first = unique.iterator().next();
        int remaining = unique.size() - 1;
        return first + " + " + remaining + " other" + (remaining == 1 ? "" : "s");
    }

    public Account createOrUpdateAccount(Long userId, Long connectionId, SimpleFinClient.SimpleFinAccount sfAccount) {
        Account account = accountRepository
                .findByConnectionIdAndExternalId(connectionId, sfAccount.id())
                .orElse(Account.builder()
                        .userId(userId)
                        .connectionId(connectionId)
                        .externalId(sfAccount.id())
                        .active(true)
                        .build());

        account.setName(sfAccount.name());
        account.setInstitutionName(sfAccount.institutionName());
        account.setAccountType(AccountType.valueOf(sfAccount.accountType()));
        account.setCurrency(sfAccount.currency() != null ? sfAccount.currency() : "USD");
        account.setCurrentBalance(sfAccount.balance());
        account.setAvailableBalance(sfAccount.availableBalance());
        if (sfAccount.balanceDate() != null) {
            account.setBalanceUpdatedAt(sfAccount.balanceDate());
        } else if (account.getBalanceUpdatedAt() == null) {
            account.setBalanceUpdatedAt(Instant.now());
        }

        return accountRepository.save(account);
    }

    public SyncTransactionResult syncTransactions(Account account, List<SimpleFinClient.SimpleFinTransaction> transactions) {
        int added = 0;
        int updated = 0;
        Long uncategorizedCategoryId = uncategorizedCategoryService.requireSystemUncategorizedCategoryId();

        for (var sfTx : transactions) {
            var existing = transactionReadRepository
                    .findByAccountIdAndExternalId(account.getId(), sfTx.id());

            if (existing.isPresent()) {
                Transaction tx = existing.get();
                boolean changed = false;

                if (tx.isPending() != sfTx.pending()) {
                    tx.setPending(sfTx.pending());
                    changed = true;
                }
                if (!tx.getAmount().equals(sfTx.amount())) {
                    tx.setAmount(sfTx.amount());
                    changed = true;
                }

                if (!tx.isManuallyCategorized() && (tx.getCategoryId() == null || tx.getCategorizedByRuleId() != null)) {
                    AutoCategorizationService.CategorizationMatch match = categorizationService.categorize(
                            account.getUserId(), account.getId(), sfTx.amount(), sfTx.description(), sfTx.payee(), sfTx.memo());
                    if (match != null) {
                        tx.setCategoryId(match.categoryId());
                        tx.setCategorizedByRuleId(match.ruleId());
                        changed = true;
                    } else {
                        tx.setCategoryId(uncategorizedCategoryId);
                        tx.setCategorizedByRuleId(null);
                        changed = true;
                    }
                }

                if (changed) {
                    transactionWriteRepository.save(tx);
                    updated++;
                }
            } else {
                Transaction tx = Transaction.builder()
                        .accountId(account.getId())
                        .externalId(sfTx.id())
                        .postedAt(sfTx.posted())
                        .transactedAt(sfTx.transacted())
                        .amount(sfTx.amount())
                        .pending(sfTx.pending())
                        .description(sfTx.description())
                        .payee(sfTx.payee())
                        .memo(sfTx.memo())
                        .build();

                AutoCategorizationService.CategorizationMatch match = categorizationService.categorize(
                        account.getUserId(), account.getId(), sfTx.amount(), sfTx.description(), sfTx.payee(), sfTx.memo());
                if (match != null) {
                    tx.setCategoryId(match.categoryId());
                    tx.setCategorizedByRuleId(match.ruleId());
                    tx.setManuallyCategorized(false);
                } else {
                    tx.setCategoryId(uncategorizedCategoryId);
                    tx.setCategorizedByRuleId(null);
                    tx.setManuallyCategorized(false);
                }

                transactionWriteRepository.save(tx);
                added++;
            }
        }

        return new SyncTransactionResult(added, updated);
    }

    public record SyncTransactionResult(int added, int updated) {}
}
