package com.peter.budget.controller;

import com.peter.budget.config.JwtAuthFilter;
import com.peter.budget.model.dto.CashFlowDto;
import com.peter.budget.model.dto.SpendingByCategoryDto;
import com.peter.budget.model.dto.TrendDto;
import com.peter.budget.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/spending")
    public ResponseEntity<SpendingByCategoryDto> getSpendingByCategory(
            @AuthenticationPrincipal JwtAuthFilter.UserPrincipal principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        SpendingByCategoryDto spending = analyticsService.getSpendingByCategory(
                principal.userId(), startDate, endDate);
        return ResponseEntity.ok(spending);
    }

    @GetMapping("/trends")
    public ResponseEntity<TrendDto> getTrends(
            @AuthenticationPrincipal JwtAuthFilter.UserPrincipal principal,
            @RequestParam(defaultValue = "6") int months) {
        TrendDto trends = analyticsService.getTrends(principal.userId(), months);
        return ResponseEntity.ok(trends);
    }

    @GetMapping("/cashflow")
    public ResponseEntity<CashFlowDto> getCashFlow(
            @AuthenticationPrincipal JwtAuthFilter.UserPrincipal principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        CashFlowDto cashFlow = analyticsService.getCashFlow(principal.userId(), startDate, endDate);
        return ResponseEntity.ok(cashFlow);
    }
}
