package com.peter.budget.service;

import com.peter.budget.model.entity.Category;
import com.peter.budget.model.entity.CategoryOverride;
import com.peter.budget.repository.CategoryOverrideRepository;
import com.peter.budget.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryViewService {

    private final CategoryRepository categoryRepository;
    private final CategoryOverrideRepository categoryOverrideRepository;

    public List<Category> getEffectiveCategoriesForUser(Long userId) {
        List<Category> categories = categoryRepository.findByUserId(userId);
        Map<Long, CategoryOverride> overridesByCategoryId = getOverridesByCategoryId(userId);

        return categories.stream()
                .filter(category -> !isHidden(category, overridesByCategoryId))
                .map(category -> applyOverride(category, overridesByCategoryId.get(category.getId())))
                .toList();
    }

    public Map<Long, Category> getEffectiveCategoryMapForUser(Long userId) {
        return getEffectiveCategoriesForUser(userId).stream()
                .collect(Collectors.toMap(Category::getId, Function.identity()));
    }

    public Optional<Category> getEffectiveCategoryByIdForUser(Long userId, Long categoryId) {
        return Optional.ofNullable(getEffectiveCategoryMapForUser(userId).get(categoryId));
    }

    private Map<Long, CategoryOverride> getOverridesByCategoryId(Long userId) {
        return categoryOverrideRepository.findByUserId(userId).stream()
                .collect(Collectors.toMap(CategoryOverride::getCategoryId, Function.identity()));
    }

    private boolean isHidden(Category category, Map<Long, CategoryOverride> overridesByCategoryId) {
        if (!category.isSystem()) {
            return false;
        }

        CategoryOverride categoryOverride = overridesByCategoryId.get(category.getId());
        return categoryOverride != null && categoryOverride.isHidden();
    }

    private Category applyOverride(Category category, CategoryOverride categoryOverride) {
        if (!category.isSystem() || categoryOverride == null) {
            return category;
        }

        return Category.builder()
                .id(category.getId())
                .userId(category.getUserId())
                .parentId(categoryOverride.getParentIdOverride())
                .name(categoryOverride.getNameOverride())
                .icon(categoryOverride.getIconOverride())
                .color(categoryOverride.getColorOverride())
                .categoryType(categoryOverride.getCategoryTypeOverride())
                .system(true)
                .sortOrder(category.getSortOrder())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }
}
