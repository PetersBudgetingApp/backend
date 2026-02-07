package com.peter.budget.service;

import com.peter.budget.exception.ApiException;
import com.peter.budget.model.dto.TransferPairDto;
import com.peter.budget.model.entity.Account;
import com.peter.budget.model.entity.Category;
import com.peter.budget.model.entity.Transaction;
import com.peter.budget.model.enums.AccountType;
import com.peter.budget.model.enums.CategoryType;
import com.peter.budget.repository.AccountRepository;
import com.peter.budget.repository.TransactionReadRepository;
import com.peter.budget.repository.TransactionWriteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferDetectionServiceTest {

    private static final long USER_ID = 7L;

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
        Transaction outgoing = baseTransaction(101L, 11L, new BigDecimal("-1000.00"));
        Transaction incoming = baseTransaction(202L, 22L, new BigDecimal("1000.00"));

        when(categoryViewService.getEffectiveCategoriesForUser(USER_ID))
                .thenReturn(List.of(Category.builder()
                        .id(999L)
                        .name("Transfer")
                        .categoryType(CategoryType.TRANSFER)
                        .build()));

        transferDetectionService.linkAsTransfer(USER_ID, outgoing, incoming);

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

    @Test
    void linkAsTransferSkipsCategorySaveForManuallyCategorizatedTransactions() {
        Transaction outgoing = baseTransaction(101L, 11L, new BigDecimal("-500.00"));
        outgoing.setManuallyCategorized(true);
        outgoing.setCategoryId(42L);
        Transaction incoming = baseTransaction(202L, 22L, new BigDecimal("500.00"));
        incoming.setManuallyCategorized(true);
        incoming.setCategoryId(43L);

        when(categoryViewService.getEffectiveCategoriesForUser(USER_ID))
                .thenReturn(List.of(Category.builder()
                        .id(999L).name("Transfer").categoryType(CategoryType.TRANSFER).build()));

        transferDetectionService.linkAsTransfer(USER_ID, outgoing, incoming);

        verify(transactionWriteRepository).linkTransferPair(outgoing.getId(), incoming.getId());
        verify(transactionWriteRepository, never()).save(argThat(tx ->
                tx.getId().equals(outgoing.getId())));
        verify(transactionWriteRepository, never()).save(argThat(tx ->
                tx.getId().equals(incoming.getId())));
        assertEquals(42L, outgoing.getCategoryId());
        assertEquals(43L, incoming.getCategoryId());
    }

    @Test
    void markAsTransferRejectsSameTransaction() {
        ApiException exception = assertThrows(
                ApiException.class,
                () -> transferDetectionService.markAsTransfer(USER_ID, 1L, 1L)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("Cannot create transfer pair from the same transaction", exception.getMessage());
    }

    @Test
    void markAsTransferRejectsSameAccount() {
        Transaction tx1 = baseTransaction(1L, 10L, new BigDecimal("-100.00"));
        Transaction tx2 = baseTransaction(2L, 10L, new BigDecimal("100.00"));

        when(transactionReadRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(tx1));
        when(transactionReadRepository.findByIdAndUserId(2L, USER_ID)).thenReturn(Optional.of(tx2));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> transferDetectionService.markAsTransfer(USER_ID, 1L, 2L)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("Transfer transactions must be from different accounts", exception.getMessage());
    }

    @Test
    void markAsTransferRejectsNonOppositeAmounts() {
        Transaction tx1 = baseTransaction(1L, 10L, new BigDecimal("-100.00"));
        Transaction tx2 = baseTransaction(2L, 20L, new BigDecimal("200.00"));

        when(transactionReadRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(tx1));
        when(transactionReadRepository.findByIdAndUserId(2L, USER_ID)).thenReturn(Optional.of(tx2));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> transferDetectionService.markAsTransfer(USER_ID, 1L, 2L)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void markAsTransferThrowsNotFoundForMissingTransaction() {
        when(transactionReadRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.empty());

        ApiException exception = assertThrows(
                ApiException.class,
                () -> transferDetectionService.markAsTransfer(USER_ID, 1L, 2L)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    void unlinkTransferThrowsNotFoundForMissingTransaction() {
        when(transactionReadRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.empty());

        ApiException exception = assertThrows(
                ApiException.class,
                () -> transferDetectionService.unlinkTransfer(USER_ID, 1L)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    void unlinkTransferThrowsBadRequestWhenNotATransferPair() {
        Transaction tx = baseTransaction(1L, 10L, new BigDecimal("-100.00"));
        tx.setTransferPairId(null);

        when(transactionReadRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(tx));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> transferDetectionService.unlinkTransfer(USER_ID, 1L)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("Transaction is not part of a transfer pair", exception.getMessage());
    }

    @Test
    void unlinkTransferDelegatesToRepository() {
        Transaction tx = baseTransaction(1L, 10L, new BigDecimal("-100.00"));
        tx.setTransferPairId(2L);

        when(transactionReadRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(tx));

        transferDetectionService.unlinkTransfer(USER_ID, 1L);

        verify(transactionWriteRepository).unlinkTransferPair(1L);
    }

    @Test
    void getTransferPairsReturnsGroupedPairs() {
        Transaction outgoing = baseTransaction(1L, 10L, new BigDecimal("-500.00"));
        outgoing.setTransferPairId(2L);
        outgoing.setDescription("Transfer out");
        Transaction incoming = baseTransaction(2L, 20L, new BigDecimal("500.00"));
        incoming.setTransferPairId(1L);

        Account fromAccount = Account.builder().id(10L).name("Checking").accountType(AccountType.CHECKING).build();
        Account toAccount = Account.builder().id(20L).name("Savings").accountType(AccountType.SAVINGS).build();

        when(transactionReadRepository.findTransfersByUserId(USER_ID)).thenReturn(List.of(outgoing, incoming));
        when(transactionReadRepository.findById(2L)).thenReturn(Optional.of(incoming));
        when(accountRepository.findById(10L)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById(20L)).thenReturn(Optional.of(toAccount));

        List<TransferPairDto> result = transferDetectionService.getTransferPairs(USER_ID);

        assertEquals(1, result.size());
        assertEquals("Checking", result.get(0).getFromAccountName());
        assertEquals("Savings", result.get(0).getToAccountName());
        assertEquals(new BigDecimal("500.00"), result.get(0).getAmount());
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
