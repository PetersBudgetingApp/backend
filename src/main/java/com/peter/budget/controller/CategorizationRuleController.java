package com.peter.budget.controller;

import com.peter.budget.config.JwtAuthFilter;
import com.peter.budget.model.dto.CategorizationRuleDto;
import com.peter.budget.model.dto.CategorizationRuleUpsertRequest;
import com.peter.budget.service.CategorizationRuleService;
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
}
