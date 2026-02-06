package com.peter.budget.model.entity;

import com.peter.budget.model.enums.MatchField;
import com.peter.budget.model.enums.PatternType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategorizationRule {
    private Long id;
    private Long userId;
    private String name;
    private String pattern;
    private PatternType patternType;
    private MatchField matchField;
    private Long categoryId;
    private int priority;
    private boolean active;
    private boolean system;
    private Instant createdAt;
    private Instant updatedAt;
}
