package com.peter.budget.model.dto;

import com.peter.budget.model.enums.CategoryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDto {
    private Long id;
    private Long parentId;
    private String name;
    private String icon;
    private String color;
    private CategoryType categoryType;
    private boolean system;
    private List<CategoryDto> children;
}
