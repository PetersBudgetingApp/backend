package com.peter.budget.controller;

import com.peter.budget.config.JwtAuthFilter;
import com.peter.budget.model.dto.CategorizationRuleBackfillResultDto;
import com.peter.budget.model.dto.CategorizationRuleDto;
import com.peter.budget.model.dto.CategorizationRuleUpsertRequest;
import com.peter.budget.model.dto.TransactionDto;
import com.peter.budget.service.CategorizationRuleService;
import com.peter.budget.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categorization-rules")
@RequiredArgsConstructor
public class CategorizationRuleController {

    private final CategorizationRuleService categorizationRuleService;
    private final TransactionService transactionService;

    @GetMapping
    public ResponseEntity<List<CategorizationRuleDto>> getRules(
            @AuthenticationPrincipal JwtAuthFilter.UserPrincipal principal) {
        return ResponseEntity.ok(categorizationRuleService.getRulesForUser(principal.userId()));
    }

    @PostMapping
    public ResponseEntity<CategorizationRuleDto> createRule(
            @AuthenticationPrincipal JwtAuthFilter.UserPrincipal principal,
            @Valid @RequestBody CategorizationRuleUpsertRequest request) {
        CategorizationRuleDto rule = categorizationRuleService.createRule(principal.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(rule);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategorizationRuleDto> updateRule(
            @AuthenticationPrincipal JwtAuthFilter.UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody CategorizationRuleUpsertRequest request) {
        CategorizationRuleDto rule = categorizationRuleService.updateRule(principal.userId(), id, request);
        return ResponseEntity.ok(rule);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRule(
            @AuthenticationPrincipal JwtAuthFilter.UserPrincipal principal,
            @PathVariable Long id) {
        categorizationRuleService.deleteRule(principal.userId(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/transactions")
    public ResponseEntity<List<TransactionDto>> getRuleTransactions(
            @AuthenticationPrincipal JwtAuthFilter.UserPrincipal principal,
            @PathVariable Long id,
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        List<TransactionDto> transactions = transactionService.getTransactionsForCategorizationRule(
                principal.userId(), id, limit, offset);
        return ResponseEntity.ok(transactions);
    }

    @PostMapping("/backfill")
    public ResponseEntity<CategorizationRuleBackfillResultDto> backfillRuleAssignments(
            @AuthenticationPrincipal JwtAuthFilter.UserPrincipal principal) {
        CategorizationRuleBackfillResultDto result = transactionService.backfillCategorizationRules(principal.userId());
        return ResponseEntity.ok(result);
    }
}
