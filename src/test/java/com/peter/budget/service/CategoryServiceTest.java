package com.peter.budget.service;

import com.peter.budget.model.entity.Category;
import com.peter.budget.model.entity.CategoryOverride;
import com.peter.budget.model.enums.CategoryType;
import com.peter.budget.repository.CategorizationRuleRepository;
import com.peter.budget.repository.CategoryOverrideRepository;
import com.peter.budget.repository.CategoryRepository;
import com.peter.budget.repository.RecurringPatternRepository;
import com.peter.budget.repository.TransactionWriteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    private static final long USER_ID = 7L;
    private static final long ROOT_ID = 100L;
    private static final long CHILD_ID = 101L;

    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private CategoryOverrideRepository categoryOverrideRepository;
    @Mock
    private TransactionWriteRepository transactionWriteRepository;
    @Mock
    private RecurringPatternRepository recurringPatternRepository;
    @Mock
    private CategorizationRuleRepository categorizationRuleRepository;
    @Mock
    private CategoryViewService categoryViewService;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void deleteNonSystemCategoryClearsBindingsForEntireDeletedTree() {
        Category root = category(ROOT_ID, null, false, "Trips");
        Category child = category(CHILD_ID, ROOT_ID, false, "Ski Trips");
        Category unrelated = category(999L, null, false, "Other");

        when(categoryViewService.getEffectiveCategoryByIdForUser(USER_ID, ROOT_ID))
                .thenReturn(Optional.of(root));
        when(categoryViewService.getEffectiveCategoriesForUser(USER_ID))
                .thenReturn(List.of(root, child, unrelated));

        categoryService.deleteCategory(USER_ID, ROOT_ID);

        ArgumentCaptor<List<Long>> idsCaptor = ArgumentCaptor.forClass(List.class);
        verify(transactionWriteRepository).clearCategoryForUserAndCategoryIds(eq(USER_ID), idsCaptor.capture());
        List<Long> removedCategoryIds = idsCaptor.getValue();
        assertEquals(Set.of(ROOT_ID, CHILD_ID), new HashSet<>(removedCategoryIds));

        verify(recurringPatternRepository).clearCategoryForUserAndCategoryIds(USER_ID, removedCategoryIds);
        verify(categorizationRuleRepository).deleteByUserIdAndCategoryIds(USER_ID, removedCategoryIds);
        verify(categoryRepository).deleteById(ROOT_ID);
        verify(categoryRepository).deleteById(CHILD_ID);
        verify(categoryOverrideRepository, never()).save(any(CategoryOverride.class));
    }

    @Test
    void deleteSystemCategoryHidesSystemNodeAndDeletesUserChildren() {
        Category rootSystem = category(ROOT_ID, null, true, "Rideshare");
        Category childUser = category(CHILD_ID, ROOT_ID, false, "Uber");

        when(categoryViewService.getEffectiveCategoryByIdForUser(USER_ID, ROOT_ID))
                .thenReturn(Optional.of(rootSystem));
        when(categoryViewService.getEffectiveCategoriesForUser(USER_ID))
                .thenReturn(List.of(rootSystem, childUser));
        when(categoryOverrideRepository.findByUserIdAndCategoryId(USER_ID, ROOT_ID))
                .thenReturn(Optional.empty());

        categoryService.deleteCategory(USER_ID, ROOT_ID);

        ArgumentCaptor<List<Long>> idsCaptor = ArgumentCaptor.forClass(List.class);
        verify(transactionWriteRepository).clearCategoryForUserAndCategoryIds(eq(USER_ID), idsCaptor.capture());
        assertEquals(Set.of(ROOT_ID, CHILD_ID), new HashSet<>(idsCaptor.getValue()));

        verify(categoryRepository).deleteById(CHILD_ID);
        verify(categoryRepository, never()).deleteById(ROOT_ID);

        ArgumentCaptor<CategoryOverride> overrideCaptor = ArgumentCaptor.forClass(CategoryOverride.class);
        verify(categoryOverrideRepository).save(overrideCaptor.capture());
        CategoryOverride saved = overrideCaptor.getValue();
        assertEquals(USER_ID, saved.getUserId());
        assertEquals(ROOT_ID, saved.getCategoryId());
        assertTrue(saved.isHidden());
    }

    private Category category(Long id, Long parentId, boolean system, String name) {
        return Category.builder()
                .id(id)
                .parentId(parentId)
                .name(name)
                .categoryType(CategoryType.EXPENSE)
                .system(system)
                .build();
    }
}
