package com.peter.budget.model.dto;

import com.peter.budget.model.enums.MatchField;
import com.peter.budget.model.enums.PatternType;
import com.peter.budget.model.enums.RuleConditionOperator;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

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
    private RuleConditionOperator conditionOperator;
    private List<CategorizationRuleConditionDto> conditions;
    private Long categoryId;
    private int priority;
    private boolean active;
    private boolean system;
}
