package com.peter.budget.controller;

import com.peter.budget.config.JwtAuthFilter;
import com.peter.budget.model.dto.ErrorResponse;
import com.peter.budget.model.dto.MarkTransferRequest;
import com.peter.budget.model.dto.MessageResponse;
import com.peter.budget.model.dto.TransactionCreateRequest;
import com.peter.budget.model.dto.TransactionDto;
import com.peter.budget.model.dto.TransactionCoverageDto;
import com.peter.budget.model.dto.TransactionUpdateRequest;
import com.peter.budget.model.dto.TransferPairDto;
import com.peter.budget.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping
    public ResponseEntity<List<TransactionDto>> getTransactions(
            @AuthenticationPrincipal JwtAuthFilter.UserPrincipal principal,
            @RequestParam(defaultValue = "false") boolean includeTransfers,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String descriptionQuery,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "false") boolean uncategorized,
            @RequestParam(required = false) Long accountId,
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        List<TransactionDto> transactions = transactionService.getTransactions(
                principal.userId(), includeTransfers, startDate, endDate, descriptionQuery, categoryId, uncategorized, accountId, limit, offset);
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/coverage")
    public ResponseEntity<TransactionCoverageDto> getTransactionCoverage(
            @AuthenticationPrincipal JwtAuthFilter.UserPrincipal principal) {
        TransactionCoverageDto coverage = transactionService.getTransactionCoverage(principal.userId());
        return ResponseEntity.ok(coverage);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionDto> getTransaction(
            @AuthenticationPrincipal JwtAuthFilter.UserPrincipal principal,
            @PathVariable Long id) {
        TransactionDto transaction = transactionService.getTransaction(principal.userId(), id);
        return ResponseEntity.ok(transaction);
    }

    @PostMapping
    public ResponseEntity<TransactionDto> createTransaction(
            @AuthenticationPrincipal JwtAuthFilter.UserPrincipal principal,
            @Valid @RequestBody TransactionCreateRequest request) {
        TransactionDto transaction = transactionService.createTransaction(principal.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(transaction);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<TransactionDto> updateTransaction(
            @AuthenticationPrincipal JwtAuthFilter.UserPrincipal principal,
            @PathVariable Long id,
            @RequestBody TransactionUpdateRequest request) {
        TransactionDto transaction = transactionService.updateTransaction(principal.userId(), id, request);
        return ResponseEntity.ok(transaction);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(
            @AuthenticationPrincipal JwtAuthFilter.UserPrincipal principal,
            @PathVariable Long id) {
        transactionService.deleteTransaction(principal.userId(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/transfers")
    public ResponseEntity<List<TransferPairDto>> getTransfers(
            @AuthenticationPrincipal JwtAuthFilter.UserPrincipal principal) {
        List<TransferPairDto> transfers = transactionService.getTransfers(principal.userId());
        return ResponseEntity.ok(transfers);
    }

    @PostMapping("/{id}/mark-as-transfer")
    public ResponseEntity<?> markAsTransfer(
            @AuthenticationPrincipal JwtAuthFilter.UserPrincipal principal,
            @PathVariable Long id,
            @RequestBody MarkTransferRequest request) {
        Long pairTransactionId = request.getPairTransactionId();
        if (pairTransactionId == null) {
            return ResponseEntity.badRequest().body(ErrorResponse.builder().error("pairTransactionId is required").build());
        }
        transactionService.markAsTransfer(principal.userId(), id, pairTransactionId);
        return ResponseEntity.ok(MessageResponse.builder().message("Transactions linked as transfer pair").build());
    }

    @PostMapping("/{id}/unlink-transfer")
    public ResponseEntity<MessageResponse> unlinkTransfer(
            @AuthenticationPrincipal JwtAuthFilter.UserPrincipal principal,
            @PathVariable Long id) {
        transactionService.unlinkTransfer(principal.userId(), id);
        return ResponseEntity.ok(MessageResponse.builder().message("Transfer pair unlinked").build());
    }
}
