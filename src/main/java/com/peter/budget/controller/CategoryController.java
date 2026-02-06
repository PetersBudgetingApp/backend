package com.peter.budget.controller;

import com.peter.budget.config.JwtAuthFilter;
import com.peter.budget.model.dto.CategoryCreateRequest;
import com.peter.budget.model.dto.CategoryDto;
import com.peter.budget.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<List<CategoryDto>> getCategories(
            @AuthenticationPrincipal JwtAuthFilter.UserPrincipal principal,
            @RequestParam(defaultValue = "false") boolean flat) {
        List<CategoryDto> categories = flat ?
                categoryService.getCategoriesFlatForUser(principal.userId()) :
                categoryService.getCategoriesForUser(principal.userId());
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryDto> getCategory(
            @AuthenticationPrincipal JwtAuthFilter.UserPrincipal principal,
            @PathVariable Long id) {
        CategoryDto category = categoryService.getCategoryById(principal.userId(), id);
        return ResponseEntity.ok(category);
    }

    @PostMapping
    public ResponseEntity<CategoryDto> createCategory(
            @AuthenticationPrincipal JwtAuthFilter.UserPrincipal principal,
            @Valid @RequestBody CategoryCreateRequest request) {
        CategoryDto category = categoryService.createCategory(principal.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(category);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryDto> updateCategory(
            @AuthenticationPrincipal JwtAuthFilter.UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody CategoryCreateRequest request) {
        CategoryDto category = categoryService.updateCategory(principal.userId(), id, request);
        return ResponseEntity.ok(category);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(
            @AuthenticationPrincipal JwtAuthFilter.UserPrincipal principal,
            @PathVariable Long id) {
        categoryService.deleteCategory(principal.userId(), id);
        return ResponseEntity.noContent().build();
    }
}
