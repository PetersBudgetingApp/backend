package com.peter.budget.controller;

import com.peter.budget.config.JwtAuthFilter;
import com.peter.budget.model.dto.AccountCreateRequest;
import com.peter.budget.model.dto.AccountDeletionPreviewDto;
import com.peter.budget.model.dto.AccountDto;
import com.peter.budget.model.dto.AccountNetWorthCategoryUpdateRequest;
import com.peter.budget.model.dto.AccountSummaryDto;
import com.peter.budget.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping
    public ResponseEntity<List<AccountDto>> getAccounts(
            @AuthenticationPrincipal JwtAuthFilter.UserPrincipal principal) {
        List<AccountDto> accounts = accountService.getAccounts(principal.userId());
        return ResponseEntity.ok(accounts);
    }

    @PostMapping
    public ResponseEntity<AccountDto> createAccount(
            @AuthenticationPrincipal JwtAuthFilter.UserPrincipal principal,
            @Valid @RequestBody AccountCreateRequest request) {
        AccountDto account = accountService.createAccount(principal.userId(), request);
        return ResponseEntity.status(201).body(account);
    }

    @GetMapping("/summary")
    public ResponseEntity<AccountSummaryDto> getAccountSummary(
            @AuthenticationPrincipal JwtAuthFilter.UserPrincipal principal) {
        AccountSummaryDto summary = accountService.getAccountSummary(principal.userId());
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountDto> getAccount(
            @AuthenticationPrincipal JwtAuthFilter.UserPrincipal principal,
            @PathVariable Long id) {
        AccountDto account = accountService.getAccount(principal.userId(), id);
        return ResponseEntity.ok(account);
    }

    @GetMapping("/{id}/deletion-preview")
    public ResponseEntity<AccountDeletionPreviewDto> getDeletionPreview(
            @AuthenticationPrincipal JwtAuthFilter.UserPrincipal principal,
            @PathVariable Long id) {
        AccountDeletionPreviewDto preview = accountService.getDeletionPreview(principal.userId(), id);
        return ResponseEntity.ok(preview);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAccount(
            @AuthenticationPrincipal JwtAuthFilter.UserPrincipal principal,
            @PathVariable Long id) {
        accountService.deleteAccount(principal.userId(), id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/net-worth-category")
    public ResponseEntity<AccountDto> updateAccountNetWorthCategory(
            @AuthenticationPrincipal JwtAuthFilter.UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody AccountNetWorthCategoryUpdateRequest request) {
        AccountDto account = accountService.updateNetWorthCategory(principal.userId(), id, request);
        return ResponseEntity.ok(account);
    }
}
