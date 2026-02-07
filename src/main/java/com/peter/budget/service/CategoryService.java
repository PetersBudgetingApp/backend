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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryOverrideRepository categoryOverrideRepository;
    private final TransactionWriteRepository transactionWriteRepository;
    private final RecurringPatternRepository recurringPatternRepository;
    private final CategorizationRuleRepository categorizationRuleRepository;
    private final CategoryViewService categoryViewService;

    public List<CategoryDto> getCategoriesForUser(Long userId) {
        List<Category> allCategories = categoryViewService.getEffectiveCategoriesForUser(userId);
        return buildCategoryTree(allCategories);
    }

    public List<CategoryDto> getCategoriesFlatForUser(Long userId) {
        return categoryViewService.getEffectiveCategoriesForUser(userId).stream()
                .map(this::toDto)
                .toList();
    }

    public CategoryDto getCategoryById(Long userId, Long categoryId) {
        Category category = categoryViewService.getEffectiveCategoryByIdForUser(userId, categoryId)
                .orElseThrow(() -> ApiException.notFound("Category not found"));
        return toDto(category);
    }

    @Transactional
    public CategoryDto createCategory(Long userId, CategoryCreateRequest request) {
        validateParentCategory(userId, request.getParentId(), null);

        Category category = Category.builder()
                .userId(userId)
                .parentId(request.getParentId())
                .name(request.getName())
                .icon(request.getIcon())
                .color(request.getColor())
                .categoryType(request.getCategoryType() != null ?
                        request.getCategoryType() : CategoryType.EXPENSE)
                .system(false)
                .sortOrder(0)
                .build();

        category = categoryRepository.save(category);
        return toDto(category);
    }

    @Transactional
    public CategoryDto updateCategory(Long userId, Long categoryId, CategoryCreateRequest request) {
        Category category = categoryRepository.findByIdForUser(categoryId, userId)
                .orElseThrow(() -> ApiException.notFound("Category not found"));

        validateParentCategory(userId, request.getParentId(), categoryId);

        if (category.isSystem()) {
            return updateSystemCategory(userId, category, request);
        }

        category.setParentId(request.getParentId());
        category.setName(request.getName());
        category.setIcon(request.getIcon());
        category.setColor(request.getColor());
        if (request.getCategoryType() != null) {
            category.setCategoryType(request.getCategoryType());
        }

        category = categoryRepository.save(category);
        return toDto(category);
    }

    @Transactional
    public void deleteCategory(Long userId, Long categoryId) {
        Category category = categoryViewService.getEffectiveCategoryByIdForUser(userId, categoryId)
                .orElseThrow(() -> ApiException.notFound("Category not found"));

        if (!category.isSystem()) {
            categoryRepository.deleteById(categoryId);
            return;
        }

        List<Category> allCategories = categoryViewService.getEffectiveCategoriesForUser(userId);
        List<Category> categoryTree = collectCategoryTree(categoryId, allCategories);
        hideSystemCategoryTreeForUser(userId, categoryTree);
    }

    private CategoryDto updateSystemCategory(Long userId, Category category, CategoryCreateRequest request) {
        CategoryOverride categoryOverride = categoryOverrideRepository.findByUserIdAndCategoryId(userId, category.getId())
                .orElse(CategoryOverride.builder()
                        .userId(userId)
                        .categoryId(category.getId())
                        .parentIdOverride(category.getParentId())
                        .nameOverride(category.getName())
                        .iconOverride(category.getIcon())
                        .colorOverride(category.getColor())
                        .categoryTypeOverride(category.getCategoryType())
                        .hidden(false)
                        .build());

        categoryOverride.setParentIdOverride(request.getParentId());
        categoryOverride.setNameOverride(request.getName());
        categoryOverride.setIconOverride(request.getIcon());
        categoryOverride.setColorOverride(request.getColor());
        categoryOverride.setCategoryTypeOverride(request.getCategoryType() != null ?
                request.getCategoryType() : category.getCategoryType());
        categoryOverride.setHidden(false);

        categoryOverrideRepository.save(categoryOverride);

        Category updatedCategory = categoryViewService.getEffectiveCategoryByIdForUser(userId, category.getId())
                .orElseThrow(() -> ApiException.notFound("Category not found"));

        return toDto(updatedCategory);
    }

    private void hideSystemCategoryTreeForUser(Long userId, List<Category> categoriesToRemove) {
        List<Long> categoryIds = categoriesToRemove.stream()
                .map(Category::getId)
                .toList();

        transactionWriteRepository.clearCategoryForUserAndCategoryIds(userId, categoryIds);
        recurringPatternRepository.clearCategoryForUserAndCategoryIds(userId, categoryIds);
        categorizationRuleRepository.deleteByUserIdAndCategoryIds(userId, categoryIds);

        for (Category category : categoriesToRemove) {
            if (!category.isSystem()) {
                categoryRepository.deleteById(category.getId());
                continue;
            }

            CategoryOverride categoryOverride = categoryOverrideRepository.findByUserIdAndCategoryId(userId, category.getId())
                    .orElse(CategoryOverride.builder()
                            .userId(userId)
                            .categoryId(category.getId())
                            .build());

            categoryOverride.setParentIdOverride(category.getParentId());
            categoryOverride.setNameOverride(category.getName());
            categoryOverride.setIconOverride(category.getIcon());
            categoryOverride.setColorOverride(category.getColor());
            categoryOverride.setCategoryTypeOverride(category.getCategoryType());
            categoryOverride.setHidden(true);

            categoryOverrideRepository.save(categoryOverride);
        }
    }

    private void validateParentCategory(Long userId, Long parentId, Long categoryId) {
        if (parentId == null) {
            return;
        }

        if (categoryId != null && parentId.equals(categoryId)) {
            throw ApiException.badRequest("Category cannot be its own parent");
        }

        categoryViewService.getEffectiveCategoryByIdForUser(userId, parentId)
                .orElseThrow(() -> ApiException.notFound("Parent category not found"));
    }

    private List<Category> collectCategoryTree(Long rootCategoryId, List<Category> allCategories) {
        Map<Long, List<Category>> childrenMap = allCategories.stream()
                .filter(category -> category.getParentId() != null)
                .collect(Collectors.groupingBy(Category::getParentId));

        Category rootCategory = allCategories.stream()
                .filter(category -> category.getId().equals(rootCategoryId))
                .findFirst()
                .orElseThrow(() -> ApiException.notFound("Category not found"));

        var queue = new ArrayDeque<Category>();
        queue.add(rootCategory);

        List<Category> collected = new ArrayList<>();
        while (!queue.isEmpty()) {
            Category current = queue.removeFirst();
            collected.add(current);
            for (Category child : childrenMap.getOrDefault(current.getId(), List.of())) {
                queue.addLast(child);
            }
        }

        return collected;
    }

    private List<CategoryDto> buildCategoryTree(List<Category> allCategories) {
        Map<Long, List<Category>> childrenMap = allCategories.stream()
                .filter(c -> c.getParentId() != null)
                .collect(Collectors.groupingBy(Category::getParentId));

        return allCategories.stream()
                .filter(c -> c.getParentId() == null)
                .map(c -> toDtoWithChildren(c, childrenMap))
                .toList();
    }

    private CategoryDto toDtoWithChildren(Category category, Map<Long, List<Category>> childrenMap) {
        List<Category> children = childrenMap.getOrDefault(category.getId(), List.of());
        List<CategoryDto> childDtos = children.stream()
                .map(c -> toDtoWithChildren(c, childrenMap))
                .toList();

        return CategoryDto.builder()
                .id(category.getId())
                .parentId(category.getParentId())
                .name(category.getName())
                .icon(category.getIcon())
                .color(category.getColor())
                .categoryType(category.getCategoryType())
                .system(category.isSystem())
                .children(childDtos.isEmpty() ? null : childDtos)
                .build();
    }

    private CategoryDto toDto(Category category) {
        return CategoryDto.builder()
                .id(category.getId())
                .parentId(category.getParentId())
                .name(category.getName())
                .icon(category.getIcon())
                .color(category.getColor())
                .categoryType(category.getCategoryType())
                .system(category.isSystem())
                .build();
    }
}
