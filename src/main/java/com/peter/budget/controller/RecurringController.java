package com.peter.budget.controller;

import com.peter.budget.config.JwtAuthFilter;
import com.peter.budget.model.dto.RecurringPatternDto;
import com.peter.budget.model.dto.UpcomingBillDto;
import com.peter.budget.service.RecurringDetectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/recurring")
@RequiredArgsConstructor
public class RecurringController {

    private final RecurringDetectionService recurringService;

    @GetMapping
    public ResponseEntity<List<RecurringPatternDto>> getRecurringPatterns(
            @AuthenticationPrincipal JwtAuthFilter.UserPrincipal principal) {
        List<RecurringPatternDto> patterns = recurringService.getRecurringPatterns(principal.userId());
        return ResponseEntity.ok(patterns);
    }

    @PostMapping("/detect")
    public ResponseEntity<Map<String, Object>> detectPatterns(
            @AuthenticationPrincipal JwtAuthFilter.UserPrincipal principal) {
        int detected = recurringService.detectRecurringPatterns(principal.userId());
        return ResponseEntity.ok(Map.of(
                "patternsDetected", detected,
                "message", detected + " recurring patterns detected"
        ));
    }

    @GetMapping("/upcoming")
    public ResponseEntity<List<UpcomingBillDto>> getUpcomingBills(
            @AuthenticationPrincipal JwtAuthFilter.UserPrincipal principal,
            @RequestParam(defaultValue = "30") int days) {
        List<UpcomingBillDto> bills = recurringService.getUpcomingBills(principal.userId(), days);
        return ResponseEntity.ok(bills);
    }

    @GetMapping("/calendar")
    public ResponseEntity<List<UpcomingBillDto>> getBillsForMonth(
            @AuthenticationPrincipal JwtAuthFilter.UserPrincipal principal,
            @RequestParam int year,
            @RequestParam int month) {
        List<UpcomingBillDto> bills = recurringService.getBillsForMonth(principal.userId(), year, month);
        return ResponseEntity.ok(bills);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<RecurringPatternDto> togglePatternActive(
            @AuthenticationPrincipal JwtAuthFilter.UserPrincipal principal,
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> request) {
        boolean active = request.getOrDefault("active", true);
        RecurringPatternDto pattern = recurringService.togglePatternActive(principal.userId(), id, active);
        return ResponseEntity.ok(pattern);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePattern(
            @AuthenticationPrincipal JwtAuthFilter.UserPrincipal principal,
            @PathVariable Long id) {
        recurringService.deletePattern(principal.userId(), id);
        return ResponseEntity.noContent().build();
    }
}
