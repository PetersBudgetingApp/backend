package com.peter.budget.service.recurring;

import com.peter.budget.exception.ApiException;
import com.peter.budget.model.dto.RecurringPatternDto;
import com.peter.budget.model.dto.UpcomingBillDto;
import com.peter.budget.model.entity.Category;
import com.peter.budget.model.entity.RecurringPattern;
import com.peter.budget.model.enums.CategoryType;
import com.peter.budget.model.enums.Frequency;
import com.peter.budget.repository.RecurringPatternRepository;
import com.peter.budget.service.CategoryViewService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecurringPatternQueryServiceTest {

    private static final long USER_ID = 7L;

    @Mock
    private RecurringPatternRepository patternRepository;
    @Mock
    private CategoryViewService categoryViewService;

    @InjectMocks
    private RecurringPatternQueryService queryService;

    @Test
    void getRecurringPatternsReturnsActivePatternsAsDtos() {
        RecurringPattern pattern = pattern(1L, "Netflix", Frequency.MONTHLY, "15.99");
        pattern.setCategoryId(10L);

        when(patternRepository.findActiveByUserId(USER_ID)).thenReturn(List.of(pattern));
        when(categoryViewService.getEffectiveCategoryByIdForUser(USER_ID, 10L))
                .thenReturn(Optional.of(Category.builder()
                        .id(10L).name("Entertainment").icon("tv").color("#FF0000")
                        .categoryType(CategoryType.EXPENSE).build()));

        List<RecurringPatternDto> result = queryService.getRecurringPatterns(USER_ID);

        assertEquals(1, result.size());
        assertEquals("Netflix", result.get(0).getName());
        assertEquals(Frequency.MONTHLY, result.get(0).getFrequency());
        assertNotNull(result.get(0).getCategory());
        assertEquals("Entertainment", result.get(0).getCategory().getName());
    }

    @Test
    void getRecurringPatternsReturnsNullCategoryWhenNotFound() {
        RecurringPattern pattern = pattern(1L, "Unknown", Frequency.MONTHLY, "10.00");
        pattern.setCategoryId(999L);

        when(patternRepository.findActiveByUserId(USER_ID)).thenReturn(List.of(pattern));
        when(categoryViewService.getEffectiveCategoryByIdForUser(USER_ID, 999L))
                .thenReturn(Optional.empty());

        List<RecurringPatternDto> result = queryService.getRecurringPatterns(USER_ID);

        assertEquals(1, result.size());
        assertNull(result.get(0).getCategory());
    }

    @Test
    void getRecurringPatternsReturnsNullCategoryWhenCategoryIdIsNull() {
        RecurringPattern pattern = pattern(1L, "No Category", Frequency.MONTHLY, "10.00");
        pattern.setCategoryId(null);

        when(patternRepository.findActiveByUserId(USER_ID)).thenReturn(List.of(pattern));

        List<RecurringPatternDto> result = queryService.getRecurringPatterns(USER_ID);

        assertEquals(1, result.size());
        assertNull(result.get(0).getCategory());
    }

    @Test
    void getUpcomingBillsReturnsSortedByDueDate() {
        RecurringPattern bill1 = pattern(1L, "Rent", Frequency.MONTHLY, "1500.00");
        bill1.setNextExpectedDate(LocalDate.now().plusDays(10));
        bill1.setBill(true);

        RecurringPattern bill2 = pattern(2L, "Netflix", Frequency.MONTHLY, "15.99");
        bill2.setNextExpectedDate(LocalDate.now().plusDays(3));
        bill2.setBill(true);

        when(patternRepository.findUpcomingBills(any(), any(), any()))
                .thenReturn(List.of(bill1, bill2));

        List<UpcomingBillDto> result = queryService.getUpcomingBills(USER_ID, 30);

        assertEquals(2, result.size());
        // Should be sorted: Netflix (3 days) before Rent (10 days)
        assertEquals("Netflix", result.get(0).getName());
        assertEquals("Rent", result.get(1).getName());
    }

    @Test
    void getUpcomingBillsMarksOverdueBills() {
        RecurringPattern overdue = pattern(1L, "Overdue Bill", Frequency.MONTHLY, "50.00");
        overdue.setNextExpectedDate(LocalDate.now().minusDays(2));
        overdue.setBill(true);

        when(patternRepository.findUpcomingBills(any(), any(), any()))
                .thenReturn(List.of(overdue));

        List<UpcomingBillDto> result = queryService.getUpcomingBills(USER_ID, 30);

        assertEquals(1, result.size());
        assertTrue(result.get(0).isOverdue());
        assertTrue(result.get(0).getDaysUntilDue() < 0);
    }

    @Test
    void getUpcomingBillsHandlesNullNextExpectedDate() {
        RecurringPattern noDate = pattern(1L, "No Date", Frequency.MONTHLY, "25.00");
        noDate.setNextExpectedDate(null);
        noDate.setBill(true);

        when(patternRepository.findUpcomingBills(any(), any(), any()))
                .thenReturn(List.of(noDate));

        List<UpcomingBillDto> result = queryService.getUpcomingBills(USER_ID, 30);

        assertEquals(1, result.size());
        assertFalse(result.get(0).isOverdue());
        assertEquals(0, result.get(0).getDaysUntilDue());
    }

    @Test
    void getBillsForMonthReturnsPatterns() {
        RecurringPattern bill = pattern(1L, "Rent", Frequency.MONTHLY, "1500.00");
        bill.setNextExpectedDate(LocalDate.of(2026, 3, 1));
        bill.setBill(true);

        when(patternRepository.findBillsForMonth(USER_ID, 2026, 3))
                .thenReturn(List.of(bill));

        List<UpcomingBillDto> result = queryService.getBillsForMonth(USER_ID, 2026, 3);

        assertEquals(1, result.size());
        assertEquals("Rent", result.get(0).getName());
    }

    @Test
    void deletePatternDeletesExistingPattern() {
        RecurringPattern existing = pattern(10L, "Old", Frequency.MONTHLY, "5.00");
        when(patternRepository.findByIdAndUserId(10L, USER_ID)).thenReturn(Optional.of(existing));

        queryService.deletePattern(USER_ID, 10L);

        verify(patternRepository).deleteById(10L);
    }

    @Test
    void deletePatternThrowsNotFoundWhenMissing() {
        when(patternRepository.findByIdAndUserId(999L, USER_ID)).thenReturn(Optional.empty());

        ApiException exception = assertThrows(
                ApiException.class,
                () -> queryService.deletePattern(USER_ID, 999L)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    void togglePatternActiveUpdatesAndReturnsDto() {
        RecurringPattern existing = pattern(10L, "Netflix", Frequency.MONTHLY, "15.99");
        existing.setActive(true);

        when(patternRepository.findByIdAndUserId(10L, USER_ID)).thenReturn(Optional.of(existing));
        when(patternRepository.save(any(RecurringPattern.class))).thenAnswer(i -> i.getArgument(0));

        RecurringPatternDto result = queryService.togglePatternActive(USER_ID, 10L, false);

        assertFalse(result.isActive());
        assertEquals("Netflix", result.getName());
    }

    @Test
    void togglePatternActiveThrowsNotFoundWhenMissing() {
        when(patternRepository.findByIdAndUserId(999L, USER_ID)).thenReturn(Optional.empty());

        ApiException exception = assertThrows(
                ApiException.class,
                () -> queryService.togglePatternActive(USER_ID, 999L, true)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    private RecurringPattern pattern(Long id, String name, Frequency frequency, String amount) {
        return RecurringPattern.builder()
                .id(id)
                .userId(USER_ID)
                .name(name)
                .merchantPattern(name.toUpperCase())
                .expectedAmount(new BigDecimal(amount))
                .frequency(frequency)
                .dayOfMonth(1)
                .nextExpectedDate(LocalDate.of(2026, 3, 1))
                .active(true)
                .bill(true)
                .lastOccurrenceAt(Instant.parse("2026-01-01T00:00:00Z"))
                .build();
    }
}
