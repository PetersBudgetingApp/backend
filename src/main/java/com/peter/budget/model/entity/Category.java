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
public class Category {
    private Long id;
    private Long userId;
    private Long parentId;
    private String name;
    private String icon;
    private String color;
    private CategoryType categoryType;
    private boolean system;
    private int sortOrder;
    private Instant createdAt;
    private Instant updatedAt;
}
