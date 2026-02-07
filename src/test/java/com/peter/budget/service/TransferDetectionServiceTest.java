package com.peter.budget.service;

import com.peter.budget.model.entity.Category;
import com.peter.budget.model.entity.Transaction;
import com.peter.budget.model.enums.CategoryType;
import com.peter.budget.repository.AccountRepository;
import com.peter.budget.repository.TransactionReadRepository;
import com.peter.budget.repository.TransactionWriteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferDetectionServiceTest {

    @Mock
    private TransactionReadRepository transactionReadRepository;
    @Mock
    private TransactionWriteRepository transactionWriteRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private CategoryViewService categoryViewService;

    @InjectMocks
    private TransferDetectionService transferDetectionService;

    @Test
    void linkAsTransferPreservesTransferFlagsWhenApplyingTransferCategory() {
        long userId = 7L;
        Transaction outgoing = baseTransaction(101L, 11L, new BigDecimal("-1000.00"));
        Transaction incoming = baseTransaction(202L, 22L, new BigDecimal("1000.00"));

        when(categoryViewService.getEffectiveCategoriesForUser(userId))
                .thenReturn(List.of(Category.builder()
                        .id(999L)
                        .name("Transfer")
                        .categoryType(CategoryType.TRANSFER)
                        .build()));

        transferDetectionService.linkAsTransfer(userId, outgoing, incoming);

        verify(transactionWriteRepository).linkTransferPair(outgoing.getId(), incoming.getId());
        verify(transactionWriteRepository).save(argThat(tx ->
                tx.getId().equals(outgoing.getId())
                        && tx.getTransferPairId().equals(incoming.getId())
                        && tx.isInternalTransfer()
                        && tx.isExcludeFromTotals()
                        && tx.getCategoryId().equals(999L)));
        verify(transactionWriteRepository).save(argThat(tx ->
                tx.getId().equals(incoming.getId())
                        && tx.getTransferPairId().equals(outgoing.getId())
                        && tx.isInternalTransfer()
                        && tx.isExcludeFromTotals()
                        && tx.getCategoryId().equals(999L)));

        assertEquals(incoming.getId(), outgoing.getTransferPairId());
        assertEquals(outgoing.getId(), incoming.getTransferPairId());
        assertTrue(outgoing.isExcludeFromTotals());
        assertTrue(incoming.isExcludeFromTotals());
    }

    private Transaction baseTransaction(Long id, Long accountId, BigDecimal amount) {
        return Transaction.builder()
                .id(id)
                .accountId(accountId)
                .amount(amount)
                .postedAt(Instant.parse("2026-02-01T00:00:00Z"))
                .build();
    }
}
