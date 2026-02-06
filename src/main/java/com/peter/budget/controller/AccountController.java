package com.peter.budget.controller;

import com.peter.budget.config.JwtAuthFilter;
import com.peter.budget.model.dto.AccountDto;
import com.peter.budget.model.dto.AccountSummaryDto;
import com.peter.budget.service.AccountService;
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
}
