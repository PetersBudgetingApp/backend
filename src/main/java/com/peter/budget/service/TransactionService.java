package com.peter.budget.service;

import com.peter.budget.exception.ApiException;
import com.peter.budget.model.dto.CategoryDto;
import com.peter.budget.model.dto.CategorizationRuleBackfillResultDto;
import com.peter.budget.model.dto.TransactionCreateRequest;
import com.peter.budget.model.dto.TransactionCoverageDto;
import com.peter.budget.model.dto.TransactionDto;
import com.peter.budget.model.dto.TransactionUpdateRequest;
import com.peter.budget.model.dto.TransferPairDto;
import com.peter.budget.model.entity.Account;
import com.peter.budget.model.entity.Category;
import com.peter.budget.model.entity.Transaction;
import com.peter.budget.repository.AccountRepository;
import com.peter.budget.repository.CategorizationRuleRepository;
import com.peter.budget.repository.TransactionReadRepository;
import com.peter.budget.repository.TransactionWriteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionReadRepository transactionReadRepository;
    private final TransactionWriteRepository transactionWriteRepository;
    private final AccountRepository accountRepository;
    private final CategorizationRuleRepository categorizationRuleRepository;
    private final AutoCategorizationService autoCategorizationService;
    private final CategoryViewService categoryViewService;
    private final TransferDetectionService transferDetectionService;

    public List<TransactionDto> getTransactions(Long userId, boolean includeTransfers,
                                                  LocalDate startDate, LocalDate endDate,
                                                  String descriptionQuery,
                                                  Long categoryId, boolean uncategorized,
                                                  Long accountId,
                                                  int limit, int offset) {
        if (uncategorized && categoryId != null) {
            throw ApiException.badRequest("Cannot filter by categoryId when uncategorized=true");
        }

        List<Transaction> transactions = transactionReadRepository.findByUserIdWithFilters(
                userId, includeTransfers, startDate, endDate, descriptionQuery, categoryId, uncategorized, accountId, limit, offset);

        Map<Long, Account> accountCache = new HashMap<>();
        Map<Long, Category> categoryMap = categoryViewService.getEffectiveCategoryMapForUser(userId);

        return transactions.stream()
                .map(tx -> toDto(tx, accountCache, categoryMap))
                .toList();
    }

    public TransactionDto getTransaction(Long userId, Long transactionId) {
        Transaction tx = transactionReadRepository.findByIdAndUserId(transactionId, userId)
                .orElseThrow(() -> ApiException.notFound("Transaction not found"));

        Map<Long, Account> accountCache = new HashMap<>();
        Map<Long, Category> categoryMap = categoryViewService.getEffectiveCategoryMapForUser(userId);

        return toDto(tx, accountCache, categoryMap);
    }

    @Transactional
    public TransactionDto createTransaction(Long userId, TransactionCreateRequest request) {
        Account account = accountRepository.findByIdAndUserId(request.getAccountId(), userId)
                .orElseThrow(() -> ApiException.notFound("Account not found"));

        if (request.getAmount().compareTo(BigDecimal.ZERO) == 0) {
            throw ApiException.badRequest("Amount must be non-zero");
        }

        Long categoryId = request.getCategoryId();
        if (categoryId != null) {
            categoryViewService.getEffectiveCategoryByIdForUser(userId, categoryId)
                    .orElseThrow(() -> ApiException.notFound("Category not found"));
        }

        String description = request.getDescription().trim();
        String payee = normalizeOptionalText(request.getPayee());
        String memo = normalizeOptionalText(request.getMemo());

        Transaction tx = Transaction.builder()
                .accountId(account.getId())
                .postedAt(toCanonicalDateInstant(request.getPostedDate()))
                .transactedAt(request.getTransactedDate() != null
                        ? toCanonicalDateInstant(request.getTransactedDate())
                        : null)
                .amount(request.getAmount())
                .pending(request.isPending())
                .description(description)
                .payee(payee)
                .memo(memo)
                .excludeFromTotals(Boolean.TRUE.equals(request.getExcludeFromTotals()))
                .notes(normalizeOptionalText(request.getNotes()))
                .build();

        if (categoryId != null) {
            tx.setCategoryId(categoryId);
            tx.setCategorizedByRuleId(null);
            tx.setManuallyCategorized(true);
        } else {
            AutoCategorizationService.CategorizationMatch match = autoCategorizationService.categorize(
                    userId, account.getId(), request.getAmount(), description, payee, memo);

            if (match != null) {
                tx.setCategoryId(match.categoryId());
                tx.setCategorizedByRuleId(match.ruleId());
                tx.setManuallyCategorized(false);
            }
        }

        Transaction saved = transactionWriteRepository.save(tx);

        Map<Long, Account> accountCache = new HashMap<>();
        Map<Long, Category> categoryMap = categoryViewService.getEffectiveCategoryMapForUser(userId);
        return toDto(saved, accountCache, categoryMap);
    }

    public List<TransactionDto> getTransactionsForCategorizationRule(Long userId, Long ruleId, int limit, int offset) {
        categorizationRuleRepository.findByIdAndUserId(ruleId, userId)
                .orElseThrow(() -> ApiException.notFound("Categorization rule not found"));

        List<Transaction> transactions = transactionReadRepository.findByUserIdAndCategorizationRuleId(
                userId, ruleId, limit, offset);

        Map<Long, Account> accountCache = new HashMap<>();
        Map<Long, Category> categoryMap = categoryViewService.getEffectiveCategoryMapForUser(userId);

        return transactions.stream()
                .map(tx -> toDto(tx, accountCache, categoryMap))
                .toList();
    }

    public TransactionCoverageDto getTransactionCoverage(Long userId) {
        var stats = transactionReadRepository.getCoverageByUserId(userId);
        return TransactionCoverageDto.builder()
                .totalTransactions(stats.totalCount())
                .oldestPostedAt(stats.oldestPostedAt())
                .newestPostedAt(stats.newestPostedAt())
                .build();
    }

    @Transactional
    public CategorizationRuleBackfillResultDto backfillCategorizationRules(Long userId) {
        List<Transaction> transactions = transactionReadRepository.findByUserId(userId);

        int eligible = 0;
        int matched = 0;
        int updated = 0;

        for (Transaction tx : transactions) {
            if (tx.isManuallyCategorized()) {
                continue;
            }
            eligible++;

            AutoCategorizationService.CategorizationMatch match = autoCategorizationService.categorize(
                    userId, tx.getAccountId(), tx.getAmount(), tx.getDescription(), tx.getPayee(), tx.getMemo());

            if (match == null) {
                if (tx.getCategorizedByRuleId() != null) {
                    tx.setCategorizedByRuleId(null);
                    tx.setCategoryId(null);
                    tx.setManuallyCategorized(false);
                    transactionWriteRepository.save(tx);
                    updated++;
                }
                continue;
            }

            matched++;

            boolean changed = false;
            if (!Objects.equals(tx.getCategoryId(), match.categoryId())) {
                tx.setCategoryId(match.categoryId());
                changed = true;
            }
            if (!Objects.equals(tx.getCategorizedByRuleId(), match.ruleId())) {
                tx.setCategorizedByRuleId(match.ruleId());
                changed = true;
            }
            if (changed) {
                tx.setManuallyCategorized(false);
                transactionWriteRepository.save(tx);
                updated++;
            }
        }

        return CategorizationRuleBackfillResultDto.builder()
                .totalTransactions(transactions.size())
                .eligibleTransactions(eligible)
                .matchedTransactions(matched)
                .updatedTransactions(updated)
                .build();
    }

    @Transactional
    public TransactionDto updateTransaction(Long userId, Long transactionId, TransactionUpdateRequest request) {
        Transaction tx = transactionReadRepository.findByIdAndUserId(transactionId, userId)
                .orElseThrow(() -> ApiException.notFound("Transaction not found"));

        if (request.isCategoryIdProvided()) {
            if (request.getCategoryId() == null) {
                tx.setCategoryId(null);
                tx.setCategorizedByRuleId(null);
                tx.setManuallyCategorized(false);
            } else {
                categoryViewService.getEffectiveCategoryByIdForUser(userId, request.getCategoryId())
                        .orElseThrow(() -> ApiException.notFound("Category not found"));
                tx.setCategoryId(request.getCategoryId());
                tx.setCategorizedByRuleId(null);
                tx.setManuallyCategorized(true);
            }
        }

        if (request.getNotes() != null) {
            tx.setNotes(request.getNotes());
        }

        if (request.getExcludeFromTotals() != null) {
            tx.setExcludeFromTotals(request.getExcludeFromTotals());
        }

        tx = transactionWriteRepository.save(tx);

        Map<Long, Account> accountCache = new HashMap<>();
        Map<Long, Category> categoryMap = categoryViewService.getEffectiveCategoryMapForUser(userId);
        return toDto(tx, accountCache, categoryMap);
    }

    @Transactional
    public void deleteTransaction(Long userId, Long transactionId) {
        Transaction tx = transactionReadRepository.findByIdAndUserId(transactionId, userId)
                .orElseThrow(() -> ApiException.notFound("Transaction not found"));

        if (tx.getExternalId() != null) {
            throw ApiException.badRequest("Only manually created transactions can be deleted");
        }

        if (tx.getTransferPairId() != null) {
            transferDetectionService.unlinkTransfer(userId, transactionId);
        }

        transactionWriteRepository.deleteById(transactionId);
    }

    public List<TransferPairDto> getTransfers(Long userId) {
        return transferDetectionService.getTransferPairs(userId);
    }

    @Transactional
    public void markAsTransfer(Long userId, Long transactionId1, Long transactionId2) {
        transferDetectionService.markAsTransfer(userId, transactionId1, transactionId2);
    }

    @Transactional
    public void unlinkTransfer(Long userId, Long transactionId) {
        transferDetectionService.unlinkTransfer(userId, transactionId);
    }

    private TransactionDto toDto(Transaction tx, Map<Long, Account> accountCache,
                                   Map<Long, Category> categoryMap) {
        Account account = accountCache.computeIfAbsent(
                tx.getAccountId(),
                id -> accountRepository.findById(id).orElse(null)
        );

        Category category = tx.getCategoryId() != null ? categoryMap.get(tx.getCategoryId()) : null;

        String transferPairAccountName = null;
        if (tx.getTransferPairId() != null) {
            Transaction pairTx = transactionReadRepository.findById(tx.getTransferPairId()).orElse(null);
            if (pairTx != null) {
                Account pairAccount = accountCache.computeIfAbsent(
                        pairTx.getAccountId(),
                        id -> accountRepository.findById(id).orElse(null)
                );
                transferPairAccountName = pairAccount != null ? pairAccount.getName() : null;
            }
        }

        return TransactionDto.builder()
                .id(tx.getId())
                .accountId(tx.getAccountId())
                .accountName(account != null ? account.getName() : null)
                .postedAt(tx.getPostedAt())
                .transactedAt(tx.getTransactedAt())
                .amount(tx.getAmount())
                .pending(tx.isPending())
                .description(tx.getDescription())
                .payee(tx.getPayee())
                .memo(tx.getMemo())
                .category(category != null ? toCategoryDto(category) : null)
                .manuallyCategorized(tx.isManuallyCategorized())
                .internalTransfer(tx.isInternalTransfer())
                .excludeFromTotals(tx.isExcludeFromTotals())
                .transferPairId(tx.getTransferPairId())
                .transferPairAccountName(transferPairAccountName)
                .recurring(tx.isRecurring())
                .notes(tx.getNotes())
                .manualEntry(tx.getExternalId() == null)
                .build();
    }

    private CategoryDto toCategoryDto(Category category) {
        return CategoryDto.builder()
                .id(category.getId())
                .parentId(category.getParentId())
                .name(category.getName())
                .icon(category.getIcon())
                .color(category.getColor())
                .categoryType(category.getCategoryType())
                .system(category.isSystem())
                .build();
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private java.time.Instant toCanonicalDateInstant(LocalDate date) {
        // Noon UTC avoids prior-day rendering for UTC-negative browser time zones.
        return date.atTime(12, 0).toInstant(ZoneOffset.UTC);
    }
}
