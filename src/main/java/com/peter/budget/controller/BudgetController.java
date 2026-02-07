package com.peter.budget.controller;

import com.peter.budget.config.JwtAuthFilter;
import com.peter.budget.model.dto.BudgetMonthDto;
import com.peter.budget.model.dto.BudgetMonthUpsertRequest;
import com.peter.budget.service.BudgetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    @GetMapping
    public ResponseEntity<BudgetMonthDto> getBudgetMonth(
            @AuthenticationPrincipal JwtAuthFilter.UserPrincipal principal,
            @RequestParam String month) {
        BudgetMonthDto budget = budgetService.getBudgetMonth(principal.userId(), month);
        return ResponseEntity.ok(budget);
    }

    @PutMapping("/{month}")
    public ResponseEntity<BudgetMonthDto> upsertBudgetMonth(
            @AuthenticationPrincipal JwtAuthFilter.UserPrincipal principal,
            @PathVariable String month,
            @Valid @RequestBody BudgetMonthUpsertRequest request) {
        BudgetMonthDto budget = budgetService.upsertBudgetMonth(principal.userId(), month, request);
        return ResponseEntity.ok(budget);
    }

    @DeleteMapping("/{month}/categories/{categoryId}")
    public ResponseEntity<Void> deleteBudgetTarget(
            @AuthenticationPrincipal JwtAuthFilter.UserPrincipal principal,
            @PathVariable String month,
            @PathVariable Long categoryId) {
        budgetService.deleteTarget(principal.userId(), month, categoryId);
        return ResponseEntity.noContent().build();
    }
}
