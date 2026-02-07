package com.peter.budget.service;

import com.peter.budget.model.entity.Category;
import com.peter.budget.model.entity.CategoryOverride;
import com.peter.budget.model.enums.CategoryType;
import com.peter.budget.repository.CategoryOverrideRepository;
import com.peter.budget.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryViewServiceTest {

    private static final long USER_ID = 7L;

    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private CategoryOverrideRepository categoryOverrideRepository;

    @InjectMocks
    private CategoryViewService categoryViewService;

    @Test
    void getEffectiveCategoriesForUserExcludesHiddenSystemCategories() {
        Category visible = systemCategory(1L, "Groceries");
        Category hidden = systemCategory(2L, "Rideshare");
        Category userCat = userCategory(3L, "Custom");

        CategoryOverride hideOverride = CategoryOverride.builder()
                .userId(USER_ID).categoryId(2L).hidden(true).build();

        when(categoryRepository.findByUserId(USER_ID))
                .thenReturn(List.of(visible, hidden, userCat));
        when(categoryOverrideRepository.findByUserId(USER_ID))
                .thenReturn(List.of(hideOverride));

        List<Category> result = categoryViewService.getEffectiveCategoriesForUser(USER_ID);

        assertEquals(2, result.size());
        assertTrue(result.stream().noneMatch(c -> c.getId().equals(2L)));
    }

    @Test
    void getEffectiveCategoriesForUserAppliesOverridesToSystemCategories() {
        Category system = systemCategory(1L, "Food");

        CategoryOverride override = CategoryOverride.builder()
                .userId(USER_ID).categoryId(1L)
                .nameOverride("My Food").iconOverride("pizza").colorOverride("#FF0000")
                .categoryTypeOverride(CategoryType.EXPENSE).hidden(false).build();

        when(categoryRepository.findByUserId(USER_ID)).thenReturn(List.of(system));
        when(categoryOverrideRepository.findByUserId(USER_ID)).thenReturn(List.of(override));

        List<Category> result = categoryViewService.getEffectiveCategoriesForUser(USER_ID);

        assertEquals(1, result.size());
        assertEquals("My Food", result.get(0).getName());
        assertEquals("pizza", result.get(0).getIcon());
        assertEquals("#FF0000", result.get(0).getColor());
        assertTrue(result.get(0).isSystem());
    }

    @Test
    void getEffectiveCategoriesForUserDoesNotApplyOverridesToUserCategories() {
        Category userCat = userCategory(3L, "Custom");

        when(categoryRepository.findByUserId(USER_ID)).thenReturn(List.of(userCat));
        when(categoryOverrideRepository.findByUserId(USER_ID)).thenReturn(List.of());

        List<Category> result = categoryViewService.getEffectiveCategoriesForUser(USER_ID);

        assertEquals(1, result.size());
        assertEquals("Custom", result.get(0).getName());
        assertFalse(result.get(0).isSystem());
    }

    @Test
    void getEffectiveCategoryMapForUserReturnsMappedById() {
        Category cat1 = userCategory(10L, "A");
        Category cat2 = userCategory(20L, "B");

        when(categoryRepository.findByUserId(USER_ID)).thenReturn(List.of(cat1, cat2));
        when(categoryOverrideRepository.findByUserId(USER_ID)).thenReturn(List.of());

        Map<Long, Category> result = categoryViewService.getEffectiveCategoryMapForUser(USER_ID);

        assertEquals(2, result.size());
        assertEquals("A", result.get(10L).getName());
        assertEquals("B", result.get(20L).getName());
    }

    @Test
    void getEffectiveCategoryByIdForUserReturnsCategory() {
        Category cat = userCategory(10L, "Test");

        when(categoryRepository.findByUserId(USER_ID)).thenReturn(List.of(cat));
        when(categoryOverrideRepository.findByUserId(USER_ID)).thenReturn(List.of());

        Optional<Category> result = categoryViewService.getEffectiveCategoryByIdForUser(USER_ID, 10L);

        assertTrue(result.isPresent());
        assertEquals("Test", result.get().getName());
    }

    @Test
    void getEffectiveCategoryByIdForUserReturnsEmptyForMissing() {
        when(categoryRepository.findByUserId(USER_ID)).thenReturn(List.of());
        when(categoryOverrideRepository.findByUserId(USER_ID)).thenReturn(List.of());

        Optional<Category> result = categoryViewService.getEffectiveCategoryByIdForUser(USER_ID, 999L);

        assertTrue(result.isEmpty());
    }

    @Test
    void getEffectiveCategoryByIdForUserReturnsEmptyForHiddenCategory() {
        Category hidden = systemCategory(5L, "Hidden");
        CategoryOverride override = CategoryOverride.builder()
                .userId(USER_ID).categoryId(5L).hidden(true).build();

        when(categoryRepository.findByUserId(USER_ID)).thenReturn(List.of(hidden));
        when(categoryOverrideRepository.findByUserId(USER_ID)).thenReturn(List.of(override));

        Optional<Category> result = categoryViewService.getEffectiveCategoryByIdForUser(USER_ID, 5L);

        assertTrue(result.isEmpty());
    }

    private Category systemCategory(Long id, String name) {
        return Category.builder()
                .id(id).userId(null).parentId(null)
                .name(name).categoryType(CategoryType.EXPENSE).system(true)
                .build();
    }

    private Category userCategory(Long id, String name) {
        return Category.builder()
                .id(id).userId(USER_ID).parentId(null)
                .name(name).categoryType(CategoryType.EXPENSE).system(false)
                .build();
    }
}
