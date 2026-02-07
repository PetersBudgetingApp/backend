package com.peter.budget.service;

import com.peter.budget.exception.ApiException;
import com.peter.budget.model.dto.TransferPairDto;
import com.peter.budget.model.entity.Account;
import com.peter.budget.model.entity.Transaction;
import com.peter.budget.model.enums.AccountType;
import com.peter.budget.model.enums.CategoryType;
import com.peter.budget.repository.AccountRepository;
import com.peter.budget.repository.TransactionReadRepository;
import com.peter.budget.repository.TransactionWriteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferDetectionService {

    private static final int TRANSFER_WINDOW_DAYS = 5;
    private static final double MATCH_THRESHOLD = 0.6;

    private static final Pattern TRANSFER_KEYWORDS = Pattern.compile(
            "(?i)(payment|transfer|xfer|ach|autopay|bill pay|internal|from.*checking|to.*savings)",
            Pattern.CASE_INSENSITIVE
    );

    private final TransactionReadRepository transactionReadRepository;
    private final TransactionWriteRepository transactionWriteRepository;
    private final AccountRepository accountRepository;
    private final CategoryViewService categoryViewService;

    @Transactional
    public int detectTransfers(Long userId) {
        List<Transaction> unpairedTransactions = transactionReadRepository.findUnpairedByUserId(userId);
        Map<Long, Account> accountCache = new HashMap<>();

        int transfersDetected = 0;

        for (Transaction tx : unpairedTransactions) {
            if (tx.getTransferPairId() != null) {
                continue;
            }

            Account txAccount = accountCache.computeIfAbsent(
                    tx.getAccountId(),
                    id -> accountRepository.findById(id).orElse(null)
            );

            if (txAccount == null) continue;

            BigDecimal oppositeAmount = tx.getAmount().negate();
            Instant startDate = tx.getPostedAt().minus(Duration.ofDays(TRANSFER_WINDOW_DAYS));
            Instant endDate = tx.getPostedAt().plus(Duration.ofDays(TRANSFER_WINDOW_DAYS));

            List<Transaction> candidates = transactionReadRepository.findPotentialTransferMatches(
                    userId, tx.getAccountId(), oppositeAmount, startDate, endDate);

            TransferCandidate bestMatch = null;
            double bestScore = 0;

            for (Transaction candidate : candidates) {
                Account candidateAccount = accountCache.computeIfAbsent(
                        candidate.getAccountId(),
                        id -> accountRepository.findById(id).orElse(null)
                );

                if (candidateAccount == null) continue;

                double score = calculateMatchScore(tx, candidate, txAccount, candidateAccount);

                if (score >= MATCH_THRESHOLD && score > bestScore) {
                    bestScore = score;
                    bestMatch = new TransferCandidate(candidate, score);
                }
            }

            if (bestMatch != null) {
                linkAsTransfer(userId, tx, bestMatch.transaction());
                transfersDetected++;
                log.debug("Linked transfer pair: {} <-> {} with score {}",
                        tx.getId(), bestMatch.transaction().getId(), bestMatch.score());
            }
        }

        return transfersDetected;
    }

    private double calculateMatchScore(Transaction tx1, Transaction tx2,
                                        Account account1, Account account2) {
        double score = 0.0;

        if (tx1.getAmount().negate().compareTo(tx2.getAmount()) == 0) {
            score += 0.4;
        }

        long daysDiff = Math.abs(Duration.between(tx1.getPostedAt(), tx2.getPostedAt()).toDays());
        if (daysDiff == 0) {
            score += 0.3;
        } else if (daysDiff <= 2) {
            score += 0.2;
        } else if (daysDiff <= 5) {
            score += 0.1;
        }

        String desc1 = (tx1.getDescription() != null ? tx1.getDescription() : "") +
                       (tx1.getMemo() != null ? " " + tx1.getMemo() : "");
        String desc2 = (tx2.getDescription() != null ? tx2.getDescription() : "") +
                       (tx2.getMemo() != null ? " " + tx2.getMemo() : "");

        if (TRANSFER_KEYWORDS.matcher(desc1).find() || TRANSFER_KEYWORDS.matcher(desc2).find()) {
            score += 0.1;
        }

        score += getAccountTypeMatchScore(account1.getAccountType(), account2.getAccountType());

        return score;
    }

    private double getAccountTypeMatchScore(AccountType type1, AccountType type2) {
        if (type1 == AccountType.CHECKING && type2 == AccountType.CREDIT_CARD) return 0.1;
        if (type1 == AccountType.CREDIT_CARD && type2 == AccountType.CHECKING) return 0.1;

        if (type1 == AccountType.CHECKING && type2 == AccountType.SAVINGS) return 0.1;
        if (type1 == AccountType.SAVINGS && type2 == AccountType.CHECKING) return 0.1;

        if (type1 == AccountType.CHECKING && type2 == AccountType.LOAN) return 0.05;
        if (type1 == AccountType.LOAN && type2 == AccountType.CHECKING) return 0.05;

        return 0.0;
    }

    @Transactional
    public void linkAsTransfer(Long userId, Transaction tx1, Transaction tx2) {
        transactionWriteRepository.linkTransferPair(tx1.getId(), tx2.getId());

        tx1.setTransferPairId(tx2.getId());
        tx2.setTransferPairId(tx1.getId());
        tx1.setInternalTransfer(true);
        tx2.setInternalTransfer(true);
        tx1.setExcludeFromTotals(true);
        tx2.setExcludeFromTotals(true);

        findTransferCategoryId(userId).ifPresent(transferCategoryId -> {
            if (!tx1.isManuallyCategorized()) {
                tx1.setCategoryId(transferCategoryId);
                tx1.setCategorizedByRuleId(null);
                transactionWriteRepository.save(tx1);
            }
            if (!tx2.isManuallyCategorized()) {
                tx2.setCategoryId(transferCategoryId);
                tx2.setCategorizedByRuleId(null);
                transactionWriteRepository.save(tx2);
            }
        });
    }

    @Transactional
    public void markAsTransfer(Long userId, Long transactionId1, Long transactionId2) {
        if (transactionId1.equals(transactionId2)) {
            throw ApiException.badRequest("Cannot create transfer pair from the same transaction");
        }

        Transaction tx1 = transactionReadRepository.findByIdAndUserId(transactionId1, userId)
                .orElseThrow(() -> ApiException.notFound("Transaction not found"));
        Transaction tx2 = transactionReadRepository.findByIdAndUserId(transactionId2, userId)
                .orElseThrow(() -> ApiException.notFound("Transaction not found"));

        if (tx1.getAccountId().equals(tx2.getAccountId())) {
            throw ApiException.badRequest("Transfer transactions must be from different accounts");
        }

        if (tx1.getAmount().add(tx2.getAmount()).compareTo(BigDecimal.ZERO) != 0) {
            throw ApiException.badRequest("Transactions must have opposite amounts to be a transfer pair");
        }

        linkAsTransfer(userId, tx1, tx2);
    }

    @Transactional
    public void unlinkTransfer(Long userId, Long transactionId) {
        Transaction tx = transactionReadRepository.findByIdAndUserId(transactionId, userId)
                .orElseThrow(() -> ApiException.notFound("Transaction not found"));

        if (tx.getTransferPairId() == null) {
            throw ApiException.badRequest("Transaction is not part of a transfer pair");
        }

        transactionWriteRepository.unlinkTransferPair(transactionId);
    }

    public List<TransferPairDto> getTransferPairs(Long userId) {
        List<Transaction> transfers = transactionReadRepository.findTransfersByUserId(userId);
        Map<Long, Account> accountCache = new HashMap<>();
        Set<Long> processedPairs = new HashSet<>();
        List<TransferPairDto> pairs = new ArrayList<>();

        for (Transaction tx : transfers) {
            if (tx.getTransferPairId() == null || processedPairs.contains(tx.getId())) {
                continue;
            }

            Transaction pairTx = transactionReadRepository.findById(tx.getTransferPairId()).orElse(null);
            if (pairTx == null) continue;

            processedPairs.add(tx.getId());
            processedPairs.add(pairTx.getId());

            Account fromAccount = accountCache.computeIfAbsent(
                    tx.getAmount().compareTo(BigDecimal.ZERO) < 0 ? tx.getAccountId() : pairTx.getAccountId(),
                    id -> accountRepository.findById(id).orElse(null)
            );
            Account toAccount = accountCache.computeIfAbsent(
                    tx.getAmount().compareTo(BigDecimal.ZERO) > 0 ? tx.getAccountId() : pairTx.getAccountId(),
                    id -> accountRepository.findById(id).orElse(null)
            );

            Transaction fromTx = tx.getAmount().compareTo(BigDecimal.ZERO) < 0 ? tx : pairTx;
            Transaction toTx = tx.getAmount().compareTo(BigDecimal.ZERO) > 0 ? tx : pairTx;

            pairs.add(TransferPairDto.builder()
                    .fromTransactionId(fromTx.getId())
                    .fromAccountName(fromAccount != null ? fromAccount.getName() : "Unknown")
                    .toTransactionId(toTx.getId())
                    .toAccountName(toAccount != null ? toAccount.getName() : "Unknown")
                    .amount(fromTx.getAmount().abs())
                    .date(fromTx.getPostedAt())
                    .description(fromTx.getDescription())
                    .autoDetected(true)
                    .build());
        }

        return pairs;
    }

    private record TransferCandidate(Transaction transaction, double score) {}

    private Optional<Long> findTransferCategoryId(Long userId) {
        return categoryViewService.getEffectiveCategoriesForUser(userId).stream()
                .filter(category -> category.getCategoryType() == CategoryType.TRANSFER)
                .map(category -> category.getId())
                .filter(Objects::nonNull)
                .findFirst();
    }
}
