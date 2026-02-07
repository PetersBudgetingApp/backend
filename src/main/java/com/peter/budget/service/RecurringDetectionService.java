package com.peter.budget.service;

import com.peter.budget.model.dto.RecurringPatternDto;
import com.peter.budget.model.dto.UpcomingBillDto;
import com.peter.budget.service.recurring.RecurringPatternApplicationService;
import com.peter.budget.service.recurring.RecurringPatternQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RecurringDetectionService {

    private final RecurringPatternApplicationService recurringPatternApplicationService;
    private final RecurringPatternQueryService recurringPatternQueryService;

    public int detectRecurringPatterns(Long userId) {
        return recurringPatternApplicationService.detectRecurringPatterns(userId);
    }

    public List<RecurringPatternDto> getRecurringPatterns(Long userId) {
        return recurringPatternQueryService.getRecurringPatterns(userId);
    }

    public List<UpcomingBillDto> getUpcomingBills(Long userId, int days) {
        return recurringPatternQueryService.getUpcomingBills(userId, days);
    }

    public List<UpcomingBillDto> getBillsForMonth(Long userId, int year, int month) {
        return recurringPatternQueryService.getBillsForMonth(userId, year, month);
    }

    public void deletePattern(Long userId, Long patternId) {
        recurringPatternQueryService.deletePattern(userId, patternId);
    }

    public RecurringPatternDto togglePatternActive(Long userId, Long patternId, boolean active) {
        return recurringPatternQueryService.togglePatternActive(userId, patternId, active);
    }
}
