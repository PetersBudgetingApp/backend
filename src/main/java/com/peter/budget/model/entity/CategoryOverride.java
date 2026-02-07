package com.peter.budget.model.entity;

import com.peter.budget.model.enums.CategoryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryOverride {
    private Long id;
    private Long userId;
    private Long categoryId;
    private Long parentIdOverride;
    private String nameOverride;
    private String iconOverride;
    private String colorOverride;
    private CategoryType categoryTypeOverride;
    private boolean hidden;
    private Instant createdAt;
    private Instant updatedAt;
}
