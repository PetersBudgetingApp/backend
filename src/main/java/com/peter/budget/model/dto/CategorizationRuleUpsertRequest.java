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
public class CategorizationRuleUpsertRequest {
    @NotBlank(message = "Rule name is required")
    @Size(max = 255, message = "Rule name must be at most 255 characters")
    private String name;

    @NotBlank(message = "Pattern is required")
    @Size(max = 500, message = "Pattern must be at most 500 characters")
    private String pattern;

    @NotNull(message = "Pattern type is required")
    private PatternType patternType;

    @NotNull(message = "Match field is required")
    private MatchField matchField;

    @NotNull(message = "Category is required")
    private Long categoryId;

    private Integer priority;
    private Boolean active;
}
