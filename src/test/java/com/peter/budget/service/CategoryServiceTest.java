package com.peter.budget.service;

import com.peter.budget.exception.ApiException;
import com.peter.budget.model.dto.CategoryCreateRequest;
import com.peter.budget.model.dto.CategoryDto;
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
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @Captor
    private ArgumentCaptor<Category> categoryCaptor;

    // --- getCategoriesForUser tests ---

    @Test
    void getCategoriesForUserReturnsTreeStructure() {
        Category root = category(ROOT_ID, null, false, "Food");
        Category child = category(CHILD_ID, ROOT_ID, false, "Groceries");

        when(categoryViewService.getEffectiveCategoriesForUser(USER_ID))
                .thenReturn(List.of(root, child));

        List<CategoryDto> result = categoryService.getCategoriesForUser(USER_ID);

        assertEquals(1, result.size());
        assertEquals("Food", result.get(0).getName());
        assertNotNull(result.get(0).getChildren());
        assertEquals(1, result.get(0).getChildren().size());
        assertEquals("Groceries", result.get(0).getChildren().get(0).getName());
    }

    @Test
    void getCategoriesFlatForUserReturnsFlatList() {
        Category root = category(ROOT_ID, null, false, "Food");
        Category child = category(CHILD_ID, ROOT_ID, false, "Groceries");

        when(categoryViewService.getEffectiveCategoriesForUser(USER_ID))
                .thenReturn(List.of(root, child));

        List<CategoryDto> result = categoryService.getCategoriesFlatForUser(USER_ID);

        assertEquals(2, result.size());
        assertNull(result.get(0).getChildren());
        assertNull(result.get(1).getChildren());
    }

    // --- getCategoryById tests ---

    @Test
    void getCategoryByIdReturnsCategory() {
        Category cat = category(ROOT_ID, null, false, "Food");

        when(categoryViewService.getEffectiveCategoryByIdForUser(USER_ID, ROOT_ID))
                .thenReturn(Optional.of(cat));

        CategoryDto result = categoryService.getCategoryById(USER_ID, ROOT_ID);

        assertEquals(ROOT_ID, result.getId());
        assertEquals("Food", result.getName());
    }

    @Test
    void getCategoryByIdThrowsNotFoundWhenMissing() {
        when(categoryViewService.getEffectiveCategoryByIdForUser(USER_ID, 999L))
                .thenReturn(Optional.empty());

        ApiException exception = assertThrows(
                ApiException.class,
                () -> categoryService.getCategoryById(USER_ID, 999L)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    // --- createCategory tests ---

    @Test
    void createCategoryCreatesNewCategory() {
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category saved = invocation.getArgument(0);
            saved.setId(200L);
            return saved;
        });

        CategoryDto result = categoryService.createCategory(USER_ID, CategoryCreateRequest.builder()
                .name("Dining")
                .icon("utensils")
                .color("#F59E0B")
                .categoryType(CategoryType.EXPENSE)
                .build());

        assertNotNull(result);
        assertEquals(200L, result.getId());
        assertEquals("Dining", result.getName());
        assertEquals("#F59E0B", result.getColor());
        assertFalse(result.isSystem());

        verify(categoryRepository).save(categoryCaptor.capture());
        Category saved = categoryCaptor.getValue();
        assertEquals(USER_ID, saved.getUserId());
        assertEquals("Dining", saved.getName());
        assertEquals(CategoryType.EXPENSE, saved.getCategoryType());
        assertFalse(saved.isSystem());
    }

    @Test
    void createCategoryDefaultsToExpenseType() {
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category saved = invocation.getArgument(0);
            saved.setId(201L);
            return saved;
        });

        categoryService.createCategory(USER_ID, CategoryCreateRequest.builder()
                .name("Test")
                .build());

        verify(categoryRepository).save(categoryCaptor.capture());
        assertEquals(CategoryType.EXPENSE, categoryCaptor.getValue().getCategoryType());
    }

    @Test
    void createCategoryWithParentValidatesParentExists() {
        when(categoryViewService.getEffectiveCategoryByIdForUser(USER_ID, ROOT_ID))
                .thenReturn(Optional.empty());

        ApiException exception = assertThrows(
                ApiException.class,
                () -> categoryService.createCategory(USER_ID, CategoryCreateRequest.builder()
                        .name("Child")
                        .parentId(ROOT_ID)
                        .build())
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        verify(categoryRepository, never()).save(any(Category.class));
    }

    // --- updateCategory tests ---

    @Test
    void updateCategoryUpdatesNonSystemCategory() {
        Category existing = category(ROOT_ID, null, false, "OldName");
        existing.setUserId(USER_ID);

        when(categoryRepository.findByIdForUser(ROOT_ID, USER_ID)).thenReturn(Optional.of(existing));
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CategoryDto result = categoryService.updateCategory(USER_ID, ROOT_ID, CategoryCreateRequest.builder()
                .name("NewName")
                .icon("star")
                .color("#FF0000")
                .categoryType(CategoryType.EXPENSE)
                .build());

        assertEquals("NewName", result.getName());
        assertEquals("star", result.getIcon());
        assertEquals("#FF0000", result.getColor());
    }

    @Test
    void updateCategoryThrowsNotFoundWhenMissing() {
        when(categoryRepository.findByIdForUser(999L, USER_ID)).thenReturn(Optional.empty());

        ApiException exception = assertThrows(
                ApiException.class,
                () -> categoryService.updateCategory(USER_ID, 999L, CategoryCreateRequest.builder()
                        .name("Test")
                        .build())
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    void updateCategoryRejectsSelfAsParent() {
        Category existing = category(ROOT_ID, null, false, "Food");
        existing.setUserId(USER_ID);

        when(categoryRepository.findByIdForUser(ROOT_ID, USER_ID)).thenReturn(Optional.of(existing));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> categoryService.updateCategory(USER_ID, ROOT_ID, CategoryCreateRequest.builder()
                        .name("Food")
                        .parentId(ROOT_ID)
                        .build())
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("Category cannot be its own parent", exception.getMessage());
    }

    // --- deleteCategory tests ---

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

    @Test
    void deleteCategoryThrowsNotFoundWhenMissing() {
        when(categoryViewService.getEffectiveCategoryByIdForUser(USER_ID, 999L))
                .thenReturn(Optional.empty());

        ApiException exception = assertThrows(
                ApiException.class,
                () -> categoryService.deleteCategory(USER_ID, 999L)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
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
