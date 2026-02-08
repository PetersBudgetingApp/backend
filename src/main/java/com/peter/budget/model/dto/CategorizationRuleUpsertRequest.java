package com.peter.budget.model.dto;

import com.peter.budget.model.enums.MatchField;
import com.peter.budget.model.enums.PatternType;
import com.peter.budget.model.enums.RuleConditionOperator;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategorizationRuleUpsertRequest {
    @NotBlank(message = "Rule name is required")
    @Size(max = 255, message = "Rule name must be at most 255 characters")
    private String name;

    @Size(max = 500, message = "Pattern must be at most 500 characters")
    private String pattern;

    private PatternType patternType;

    private MatchField matchField;

    private RuleConditionOperator conditionOperator;

    @Valid
    private List<CategorizationRuleConditionRequest> conditions;

    @NotNull(message = "Category is required")
    private Long categoryId;

    private Integer priority;
    private Boolean active;
}
