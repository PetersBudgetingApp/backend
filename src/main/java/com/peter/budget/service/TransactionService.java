package com.peter.budget.service;

import com.peter.budget.exception.ApiException;
import com.peter.budget.model.dto.CategoryDto;
import com.peter.budget.model.dto.TransactionCoverageDto;
import com.peter.budget.model.dto.TransactionDto;
import com.peter.budget.model.dto.TransactionUpdateRequest;
import com.peter.budget.model.dto.TransferPairDto;
import com.peter.budget.model.entity.Account;
import com.peter.budget.model.entity.Category;
import com.peter.budget.model.entity.Transaction;
import com.peter.budget.repository.AccountRepository;
import com.peter.budget.repository.CategoryRepository;
import com.peter.budget.repository.TransactionReadRepository;
import com.peter.budget.repository.TransactionWriteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionReadRepository transactionReadRepository;
    private final TransactionWriteRepository transactionWriteRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final TransferDetectionService transferDetectionService;

    public List<TransactionDto> getTransactions(Long userId, boolean includeTransfers,
                                                  LocalDate startDate, LocalDate endDate,
                                                  Long categoryId, Long accountId,
                                                  int limit, int offset) {
        List<Transaction> transactions = transactionReadRepository.findByUserIdWithFilters(
                userId, includeTransfers, startDate, endDate, categoryId, accountId, limit, offset);

        Map<Long, Account> accountCache = new HashMap<>();
        Map<Long, Category> categoryCache = new HashMap<>();

        return transactions.stream()
                .map(tx -> toDto(tx, accountCache, categoryCache))
                .toList();
    }

    public TransactionDto getTransaction(Long userId, Long transactionId) {
        Transaction tx = transactionReadRepository.findByIdAndUserId(transactionId, userId)
                .orElseThrow(() -> ApiException.notFound("Transaction not found"));

        Map<Long, Account> accountCache = new HashMap<>();
        Map<Long, Category> categoryCache = new HashMap<>();

        return toDto(tx, accountCache, categoryCache);
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
    public TransactionDto updateTransaction(Long userId, Long transactionId, TransactionUpdateRequest request) {
        Transaction tx = transactionReadRepository.findByIdAndUserId(transactionId, userId)
                .orElseThrow(() -> ApiException.notFound("Transaction not found"));

        if (request.isCategoryIdProvided()) {
            if (request.getCategoryId() == null) {
                tx.setCategoryId(null);
                tx.setManuallyCategorized(false);
            } else {
                categoryRepository.findByIdForUser(request.getCategoryId(), userId)
                        .orElseThrow(() -> ApiException.notFound("Category not found"));
                tx.setCategoryId(request.getCategoryId());
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
        Map<Long, Category> categoryCache = new HashMap<>();
        return toDto(tx, accountCache, categoryCache);
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
                                   Map<Long, Category> categoryCache) {
        Account account = accountCache.computeIfAbsent(
                tx.getAccountId(),
                id -> accountRepository.findById(id).orElse(null)
        );

        Category category = null;
        if (tx.getCategoryId() != null) {
            category = categoryCache.computeIfAbsent(
                    tx.getCategoryId(),
                    id -> categoryRepository.findById(id).orElse(null)
            );
        }

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
}
