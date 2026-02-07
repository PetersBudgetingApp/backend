package com.peter.budget.model.dto;

import com.peter.budget.model.enums.MatchField;
import com.peter.budget.model.enums.PatternType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategorizationRuleDto {
    private Long id;
    private String name;
    private String pattern;
    private PatternType patternType;
    private MatchField matchField;
    private Long categoryId;
    private int priority;
    private boolean active;
    private boolean system;
}
