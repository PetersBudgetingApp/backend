package com.peter.budget.service;

import com.peter.budget.exception.ApiException;
import com.peter.budget.model.dto.CategoryCreateRequest;
import com.peter.budget.model.dto.CategoryDto;
import com.peter.budget.model.entity.Category;
import com.peter.budget.model.enums.CategoryType;
import com.peter.budget.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<CategoryDto> getCategoriesForUser(Long userId) {
        List<Category> allCategories = categoryRepository.findByUserId(userId);
        return buildCategoryTree(allCategories);
    }

    public List<CategoryDto> getCategoriesFlatForUser(Long userId) {
        return categoryRepository.findByUserId(userId).stream()
                .map(this::toDto)
                .toList();
    }

    public CategoryDto getCategoryById(Long userId, Long categoryId) {
        Category category = categoryRepository.findByIdForUser(categoryId, userId)
                .orElseThrow(() -> ApiException.notFound("Category not found"));
        return toDto(category);
    }

    @Transactional
    public CategoryDto createCategory(Long userId, CategoryCreateRequest request) {
        if (request.getParentId() != null) {
            categoryRepository.findByIdForUser(request.getParentId(), userId)
                    .orElseThrow(() -> ApiException.notFound("Parent category not found"));
        }

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

        if (category.isSystem()) {
            throw ApiException.forbidden("Cannot modify system categories");
        }

        if (request.getParentId() != null) {
            if (request.getParentId().equals(categoryId)) {
                throw ApiException.badRequest("Category cannot be its own parent");
            }
            categoryRepository.findByIdForUser(request.getParentId(), userId)
                    .orElseThrow(() -> ApiException.notFound("Parent category not found"));
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
        Category category = categoryRepository.findByIdForUser(categoryId, userId)
                .orElseThrow(() -> ApiException.notFound("Category not found"));

        if (category.isSystem()) {
            throw ApiException.forbidden("Cannot delete system categories");
        }

        if (category.getUserId() == null) {
            throw ApiException.forbidden("Cannot delete system categories");
        }

        categoryRepository.deleteById(categoryId);
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
