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
public class CategorizationRuleConditionDto {
    private MatchField field;
    private PatternType patternType;
    private String value;
}
