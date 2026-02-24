package com.peter.budget.service;

import com.peter.budget.exception.ApiException;
import com.peter.budget.model.dto.BudgetMonthDto;
import com.peter.budget.model.dto.BudgetMonthUpsertRequest;
import com.peter.budget.model.dto.BudgetTargetUpsertRequest;
import com.peter.budget.model.entity.BudgetTarget;
import com.peter.budget.model.entity.Category;
import com.peter.budget.model.enums.CategoryType;
import com.peter.budget.repository.BudgetTargetRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    private static final long USER_ID = 99L;
    private static final long CATEGORY_ID = 12L;
    private static final long UNCATEGORIZED_CATEGORY_ID = 13L;

    @Mock
    private BudgetTargetRepository budgetTargetRepository;
    @Mock
    private CategoryViewService categoryViewService;

    @InjectMocks
    private BudgetService budgetService;

    @Captor
    private ArgumentCaptor<List<BudgetTargetRepository.UpsertBudgetTarget>> targetsCaptor;

    @Test
    void getBudgetMonthReturnsTargetsForMonth() {
        when(budgetTargetRepository.findByUserIdAndMonthKey(USER_ID, "2026-02"))
                .thenReturn(List.of(BudgetTarget.builder()
                        .userId(USER_ID)
                        .monthKey("2026-02")
                        .categoryId(CATEGORY_ID)
                        .targetAmount(new BigDecimal("500.00"))
                        .notes("Groceries")
                        .build()));

        BudgetMonthDto result = budgetService.getBudgetMonth(USER_ID, "2026-02");

        assertEquals("2026-02", result.getMonth());
        assertEquals("USD", result.getCurrency());
        assertEquals(1, result.getTargets().size());
        assertEquals(CATEGORY_ID, result.getTargets().get(0).getCategoryId());
        assertEquals(new BigDecimal("500.00"), result.getTargets().get(0).getTargetAmount());
    }

    @Test
    void upsertBudgetMonthReplacesMonthTargets() {
        when(categoryViewService.getEffectiveCategoryMapForUser(USER_ID))
                .thenReturn(Map.of(CATEGORY_ID, Category.builder()
                        .id(CATEGORY_ID)
                        .categoryType(CategoryType.EXPENSE)
                        .build()));
        when(budgetTargetRepository.findByUserIdAndMonthKey(USER_ID, "2026-02"))
                .thenReturn(List.of(BudgetTarget.builder()
                        .userId(USER_ID)
                        .monthKey("2026-02")
                        .categoryId(CATEGORY_ID)
                        .targetAmount(new BigDecimal("500.00"))
                        .notes("Groceries")
                        .build()));

        budgetService.upsertBudgetMonth(
                USER_ID,
                "2026-02",
                BudgetMonthUpsertRequest.builder()
                        .targets(List.of(BudgetTargetUpsertRequest.builder()
                                .categoryId(CATEGORY_ID)
                                .targetAmount(new BigDecimal("500.00"))
                                .notes("  Groceries  ")
                                .build()))
                        .build()
        );

        verify(budgetTargetRepository).replaceMonthTargets(
                eq(USER_ID),
                eq("2026-02"),
                targetsCaptor.capture()
        );
        List<BudgetTargetRepository.UpsertBudgetTarget> targets = targetsCaptor.getValue();
        assertEquals(1, targets.size());
        assertEquals(CATEGORY_ID, targets.get(0).categoryId());
        assertEquals(new BigDecimal("500.00"), targets.get(0).targetAmount());
        assertEquals("Groceries", targets.get(0).notes());
    }

    @Test
    void upsertBudgetMonthAllowsUncategorizedCategory() {
        when(categoryViewService.getEffectiveCategoryMapForUser(USER_ID))
                .thenReturn(Map.of(UNCATEGORIZED_CATEGORY_ID, Category.builder()
                        .id(UNCATEGORIZED_CATEGORY_ID)
                        .categoryType(CategoryType.UNCATEGORIZED)
                        .build()));
        when(budgetTargetRepository.findByUserIdAndMonthKey(USER_ID, "2026-02"))
                .thenReturn(List.of());

        budgetService.upsertBudgetMonth(
                USER_ID,
                "2026-02",
                BudgetMonthUpsertRequest.builder()
                        .targets(List.of(BudgetTargetUpsertRequest.builder()
                                .categoryId(UNCATEGORIZED_CATEGORY_ID)
                                .targetAmount(new BigDecimal("125.00"))
                                .build()))
                        .build()
        );

        verify(budgetTargetRepository).replaceMonthTargets(
                eq(USER_ID),
                eq("2026-02"),
                targetsCaptor.capture()
        );
        assertEquals(UNCATEGORIZED_CATEGORY_ID, targetsCaptor.getValue().get(0).categoryId());
    }

    @Test
    void upsertBudgetMonthRejectsNonExpenseCategory() {
        when(categoryViewService.getEffectiveCategoryMapForUser(USER_ID))
                .thenReturn(Map.of(CATEGORY_ID, Category.builder()
                        .id(CATEGORY_ID)
                        .categoryType(CategoryType.INCOME)
                        .build()));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> budgetService.upsertBudgetMonth(
                        USER_ID,
                        "2026-02",
                        BudgetMonthUpsertRequest.builder()
                                .targets(List.of(BudgetTargetUpsertRequest.builder()
                                        .categoryId(CATEGORY_ID)
                                        .targetAmount(new BigDecimal("500.00"))
                                        .build()))
                                .build()
                )
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        verify(budgetTargetRepository, never()).replaceMonthTargets(eq(USER_ID), eq("2026-02"), anyList());
    }

    @Test
    void upsertBudgetMonthRejectsDuplicateCategoryIds() {
        when(categoryViewService.getEffectiveCategoryMapForUser(USER_ID))
                .thenReturn(Map.of(CATEGORY_ID, Category.builder()
                        .id(CATEGORY_ID)
                        .categoryType(CategoryType.EXPENSE)
                        .build()));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> budgetService.upsertBudgetMonth(
                        USER_ID,
                        "2026-02",
                        BudgetMonthUpsertRequest.builder()
                                .targets(List.of(
                                        BudgetTargetUpsertRequest.builder()
                                                .categoryId(CATEGORY_ID)
                                                .targetAmount(new BigDecimal("500.00"))
                                                .build(),
                                        BudgetTargetUpsertRequest.builder()
                                                .categoryId(CATEGORY_ID)
                                                .targetAmount(new BigDecimal("300.00"))
                                                .build()))
                                .build()
                )
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertTrue(exception.getMessage().contains("Duplicate categoryId"));
    }

    @Test
    void upsertBudgetMonthRejectsUnknownCategory() {
        when(categoryViewService.getEffectiveCategoryMapForUser(USER_ID))
                .thenReturn(Map.of());

        ApiException exception = assertThrows(
                ApiException.class,
                () -> budgetService.upsertBudgetMonth(
                        USER_ID,
                        "2026-02",
                        BudgetMonthUpsertRequest.builder()
                                .targets(List.of(BudgetTargetUpsertRequest.builder()
                                        .categoryId(999L)
                                        .targetAmount(new BigDecimal("500.00"))
                                        .build()))
                                .build()
                )
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertTrue(exception.getMessage().contains("Category not found"));
    }

    @Test
    void upsertBudgetMonthHandlesNullTargets() {
        when(budgetTargetRepository.findByUserIdAndMonthKey(USER_ID, "2026-02"))
                .thenReturn(List.of());

        BudgetMonthDto result = budgetService.upsertBudgetMonth(
                USER_ID,
                "2026-02",
                BudgetMonthUpsertRequest.builder().targets(null).build()
        );

        verify(budgetTargetRepository).replaceMonthTargets(eq(USER_ID), eq("2026-02"), eq(List.of()));
        assertEquals("2026-02", result.getMonth());
    }

    @Test
    void upsertBudgetMonthTrimsAndNormalizesNotes() {
        when(categoryViewService.getEffectiveCategoryMapForUser(USER_ID))
                .thenReturn(Map.of(CATEGORY_ID, Category.builder()
                        .id(CATEGORY_ID)
                        .categoryType(CategoryType.EXPENSE)
                        .build()));
        when(budgetTargetRepository.findByUserIdAndMonthKey(USER_ID, "2026-02"))
                .thenReturn(List.of());

        budgetService.upsertBudgetMonth(
                USER_ID,
                "2026-02",
                BudgetMonthUpsertRequest.builder()
                        .targets(List.of(BudgetTargetUpsertRequest.builder()
                                .categoryId(CATEGORY_ID)
                                .targetAmount(new BigDecimal("100.00"))
                                .notes("   ")
                                .build()))
                        .build()
        );

        verify(budgetTargetRepository).replaceMonthTargets(
                eq(USER_ID),
                eq("2026-02"),
                targetsCaptor.capture()
        );
        assertNull(targetsCaptor.getValue().get(0).notes());
    }

    @Test
    void getBudgetMonthRejectsBadMonthFormat() {
        ApiException exception = assertThrows(
                ApiException.class,
                () -> budgetService.getBudgetMonth(USER_ID, "not-a-month")
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void getBudgetMonthRejectsNullMonth() {
        ApiException exception = assertThrows(
                ApiException.class,
                () -> budgetService.getBudgetMonth(USER_ID, null)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void getBudgetMonthRejectsBlankMonth() {
        ApiException exception = assertThrows(
                ApiException.class,
                () -> budgetService.getBudgetMonth(USER_ID, "  ")
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void deleteTargetNormalizesMonth() {
        budgetService.deleteTarget(USER_ID, "2026-02", CATEGORY_ID);

        verify(budgetTargetRepository).deleteByUserIdAndMonthKeyAndCategoryId(USER_ID, "2026-02", CATEGORY_ID);
    }
}
