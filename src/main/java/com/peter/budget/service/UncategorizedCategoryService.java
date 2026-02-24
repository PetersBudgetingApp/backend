package com.peter.budget.service;

import com.peter.budget.exception.ApiException;
import com.peter.budget.model.entity.Category;
import com.peter.budget.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UncategorizedCategoryService {

    private final CategoryRepository categoryRepository;

    public Category requireSystemUncategorizedCategory() {
        return categoryRepository.findSystemUncategorizedCategory()
                .orElseThrow(() -> ApiException.internal("System Uncategorized category is missing"));
    }

    public Long requireSystemUncategorizedCategoryId() {
        return requireSystemUncategorizedCategory().getId();
    }
}
