package com.peter.budget.service;

import com.peter.budget.model.dto.RecurringPatternDto;
import com.peter.budget.model.dto.UpcomingBillDto;
import com.peter.budget.service.recurring.RecurringPatternApplicationService;
import com.peter.budget.service.recurring.RecurringPatternQueryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecurringDetectionServiceTest {

    private static final long USER_ID = 7L;

    @Mock
    private RecurringPatternApplicationService recurringPatternApplicationService;
    @Mock
    private RecurringPatternQueryService recurringPatternQueryService;

    @InjectMocks
    private RecurringDetectionService recurringDetectionService;

    @Test
    void detectRecurringPatternsDelegatesToApplicationService() {
        when(recurringPatternApplicationService.detectRecurringPatterns(USER_ID)).thenReturn(5);

        int result = recurringDetectionService.detectRecurringPatterns(USER_ID);

        assertEquals(5, result);
        verify(recurringPatternApplicationService).detectRecurringPatterns(USER_ID);
    }

    @Test
    void getRecurringPatternsDelegatesToQueryService() {
        List<RecurringPatternDto> patterns = List.of(RecurringPatternDto.builder()
                .id(1L).name("Netflix").expectedAmount(new BigDecimal("15.99")).build());

        when(recurringPatternQueryService.getRecurringPatterns(USER_ID)).thenReturn(patterns);

        List<RecurringPatternDto> result = recurringDetectionService.getRecurringPatterns(USER_ID);

        assertEquals(1, result.size());
        assertEquals("Netflix", result.get(0).getName());
    }

    @Test
    void getUpcomingBillsDelegatesToQueryService() {
        List<UpcomingBillDto> bills = List.of(UpcomingBillDto.builder()
                .name("Rent").expectedAmount(new BigDecimal("1500.00"))
                .dueDate(LocalDate.of(2026, 2, 15)).daysUntilDue(8).overdue(false).build());

        when(recurringPatternQueryService.getUpcomingBills(USER_ID, 30)).thenReturn(bills);

        List<UpcomingBillDto> result = recurringDetectionService.getUpcomingBills(USER_ID, 30);

        assertEquals(1, result.size());
        assertEquals("Rent", result.get(0).getName());
    }

    @Test
    void getBillsForMonthDelegatesToQueryService() {
        when(recurringPatternQueryService.getBillsForMonth(USER_ID, 2026, 2)).thenReturn(List.of());

        List<UpcomingBillDto> result = recurringDetectionService.getBillsForMonth(USER_ID, 2026, 2);

        assertEquals(0, result.size());
        verify(recurringPatternQueryService).getBillsForMonth(USER_ID, 2026, 2);
    }

    @Test
    void deletePatternDelegatesToQueryService() {
        recurringDetectionService.deletePattern(USER_ID, 10L);

        verify(recurringPatternQueryService).deletePattern(USER_ID, 10L);
    }

    @Test
    void togglePatternActiveDelegatesToQueryService() {
        RecurringPatternDto dto = RecurringPatternDto.builder()
                .id(10L).name("Netflix").active(false).build();

        when(recurringPatternQueryService.togglePatternActive(USER_ID, 10L, false)).thenReturn(dto);

        RecurringPatternDto result = recurringDetectionService.togglePatternActive(USER_ID, 10L, false);

        assertEquals(10L, result.getId());
        assertEquals(false, result.isActive());
    }
}
