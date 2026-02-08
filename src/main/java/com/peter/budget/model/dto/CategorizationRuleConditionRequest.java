package com.peter.budget.model.dto;

import com.peter.budget.model.enums.MatchField;
import com.peter.budget.model.enums.PatternType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategorizationRuleConditionRequest {
    @NotNull(message = "Condition field is required")
    private MatchField field;

    @NotNull(message = "Condition pattern type is required")
    private PatternType patternType;

    @NotBlank(message = "Condition value is required")
    @Size(max = 500, message = "Condition value must be at most 500 characters")
    private String value;
}
