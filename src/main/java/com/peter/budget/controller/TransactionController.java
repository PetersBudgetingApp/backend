package com.peter.budget.controller;

import com.peter.budget.config.JwtAuthFilter;
import com.peter.budget.model.dto.TransactionDto;
import com.peter.budget.model.dto.TransactionCoverageDto;
import com.peter.budget.model.dto.TransactionUpdateRequest;
import com.peter.budget.model.dto.TransferPairDto;
import com.peter.budget.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

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
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long accountId,
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        List<TransactionDto> transactions = transactionService.getTransactions(
                principal.userId(), includeTransfers, startDate, endDate, categoryId, accountId, limit, offset);
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

    @PatchMapping("/{id}")
    public ResponseEntity<TransactionDto> updateTransaction(
            @AuthenticationPrincipal JwtAuthFilter.UserPrincipal principal,
            @PathVariable Long id,
            @RequestBody TransactionUpdateRequest request) {
        TransactionDto transaction = transactionService.updateTransaction(principal.userId(), id, request);
        return ResponseEntity.ok(transaction);
    }

    @GetMapping("/transfers")
    public ResponseEntity<List<TransferPairDto>> getTransfers(
            @AuthenticationPrincipal JwtAuthFilter.UserPrincipal principal) {
        List<TransferPairDto> transfers = transactionService.getTransfers(principal.userId());
        return ResponseEntity.ok(transfers);
    }

    @PostMapping("/{id}/mark-as-transfer")
    public ResponseEntity<Map<String, String>> markAsTransfer(
            @AuthenticationPrincipal JwtAuthFilter.UserPrincipal principal,
            @PathVariable Long id,
            @RequestBody Map<String, Long> request) {
        Long pairTransactionId = request.get("pairTransactionId");
        if (pairTransactionId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "pairTransactionId is required"));
        }
        transactionService.markAsTransfer(principal.userId(), id, pairTransactionId);
        return ResponseEntity.ok(Map.of("message", "Transactions linked as transfer pair"));
    }

    @PostMapping("/{id}/unlink-transfer")
    public ResponseEntity<Map<String, String>> unlinkTransfer(
            @AuthenticationPrincipal JwtAuthFilter.UserPrincipal principal,
            @PathVariable Long id) {
        transactionService.unlinkTransfer(principal.userId(), id);
        return ResponseEntity.ok(Map.of("message", "Transfer pair unlinked"));
    }
}
