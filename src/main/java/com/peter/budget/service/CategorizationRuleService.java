package com.peter.budget.service;

import com.peter.budget.exception.ApiException;
import com.peter.budget.model.dto.CategorizationRuleConditionDto;
import com.peter.budget.model.dto.CategorizationRuleConditionRequest;
import com.peter.budget.model.dto.CategorizationRuleDto;
import com.peter.budget.model.dto.CategorizationRuleUpsertRequest;
import com.peter.budget.model.entity.CategorizationRule;
import com.peter.budget.model.entity.CategorizationRuleCondition;
import com.peter.budget.model.enums.PatternType;
import com.peter.budget.model.enums.RuleConditionOperator;
import com.peter.budget.repository.CategorizationRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CategorizationRuleService {
    private static final Set<PatternType> TEXT_PATTERN_TYPES = Set.of(
            PatternType.CONTAINS,
            PatternType.STARTS_WITH,
            PatternType.ENDS_WITH,
            PatternType.EXACT,
            PatternType.REGEX
    );
    private static final Set<PatternType> ACCOUNT_PATTERN_TYPES = Set.of(PatternType.EXACT, PatternType.EQUALS);
    private static final Set<PatternType> AMOUNT_PATTERN_TYPES = Set.of(
            PatternType.EXACT,
            PatternType.EQUALS,
            PatternType.GREATER_THAN,
            PatternType.GREATER_THAN_OR_EQUAL,
            PatternType.LESS_THAN,
            PatternType.LESS_THAN_OR_EQUAL
    );

    private final CategorizationRuleRepository categorizationRuleRepository;
    private final CategoryViewService categoryViewService;
    private final TransactionService transactionService;

    public List<CategorizationRuleDto> getRulesForUser(Long userId) {
        return categorizationRuleRepository.findByUserId(userId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public CategorizationRuleDto createRule(Long userId, CategorizationRuleUpsertRequest request) {
        assertCategoryAccessible(userId, request.getCategoryId());
        RuleConditionsNormalized normalized = normalizeConditions(request);
        CategorizationRuleCondition primaryCondition = normalized.conditions().get(0);

        CategorizationRule rule = CategorizationRule.builder()
                .userId(userId)
                .name(request.getName().trim())
                .pattern(primaryCondition.getValue())
                .patternType(primaryCondition.getPatternType())
                .matchField(primaryCondition.getField())
                .conditionOperator(normalized.operator())
                .conditions(normalized.conditions())
                .categoryId(request.getCategoryId())
                .priority(request.getPriority() != null ? request.getPriority() : 0)
                .active(request.getActive() == null || request.getActive())
                .system(false)
                .build();

        rule = categorizationRuleRepository.save(rule);
        transactionService.backfillCategorizationRules(userId);
        return toDto(rule);
    }

    @Transactional
    public CategorizationRuleDto updateRule(Long userId, Long ruleId, CategorizationRuleUpsertRequest request) {
        CategorizationRule rule = categorizationRuleRepository.findByIdAndUserId(ruleId, userId)
                .orElseThrow(() -> ApiException.notFound("Categorization rule not found"));

        if (rule.isSystem()) {
            throw ApiException.forbidden("Cannot modify system categorization rules");
        }

        assertCategoryAccessible(userId, request.getCategoryId());
        RuleConditionsNormalized normalized = normalizeConditions(request);
        CategorizationRuleCondition primaryCondition = normalized.conditions().get(0);

        rule.setName(request.getName().trim());
        rule.setPattern(primaryCondition.getValue());
        rule.setPatternType(primaryCondition.getPatternType());
        rule.setMatchField(primaryCondition.getField());
        rule.setConditionOperator(normalized.operator());
        rule.setConditions(normalized.conditions());
        rule.setCategoryId(request.getCategoryId());
        rule.setPriority(request.getPriority() != null ? request.getPriority() : rule.getPriority());
        rule.setActive(request.getActive() != null ? request.getActive() : rule.isActive());

        rule = categorizationRuleRepository.save(rule);
        return toDto(rule);
    }

    @Transactional
    public void deleteRule(Long userId, Long ruleId) {
        CategorizationRule rule = categorizationRuleRepository.findByIdAndUserId(ruleId, userId)
                .orElseThrow(() -> ApiException.notFound("Categorization rule not found"));

        if (rule.isSystem()) {
            throw ApiException.forbidden("Cannot delete system categorization rules");
        }

        categorizationRuleRepository.deleteById(rule.getId());
    }

    private void assertCategoryAccessible(Long userId, Long categoryId) {
        categoryViewService.getEffectiveCategoryByIdForUser(userId, categoryId)
                .orElseThrow(() -> ApiException.notFound("Category not found"));
    }

    private CategorizationRuleDto toDto(CategorizationRule rule) {
        List<CategorizationRuleCondition> effectiveConditions = getEffectiveConditions(rule);

        return CategorizationRuleDto.builder()
                .id(rule.getId())
                .name(rule.getName())
                .pattern(rule.getPattern())
                .patternType(rule.getPatternType())
                .matchField(rule.getMatchField())
                .conditionOperator(rule.getConditionOperator() != null ? rule.getConditionOperator() : RuleConditionOperator.AND)
                .conditions(effectiveConditions.stream()
                        .map(condition -> CategorizationRuleConditionDto.builder()
                                .field(condition.getField())
                                .patternType(condition.getPatternType())
                                .value(condition.getValue())
                                .build())
                        .toList())
                .categoryId(rule.getCategoryId())
                .priority(rule.getPriority())
                .active(rule.isActive())
                .system(rule.isSystem())
                .build();
    }

    private RuleConditionsNormalized normalizeConditions(CategorizationRuleUpsertRequest request) {
        List<CategorizationRuleCondition> conditions;

        if (request.getConditions() != null && !request.getConditions().isEmpty()) {
            conditions = request.getConditions().stream()
                    .map(this::toCondition)
                    .toList();
        } else {
            if (request.getMatchField() == null || request.getPatternType() == null || request.getPattern() == null
                    || request.getPattern().isBlank()) {
                throw ApiException.badRequest("At least one rule condition is required");
            }

            conditions = List.of(CategorizationRuleCondition.builder()
                    .field(request.getMatchField())
                    .patternType(request.getPatternType())
                    .value(request.getPattern().trim())
                    .build());
        }

        conditions.forEach(this::validateCondition);
        RuleConditionOperator operator = request.getConditionOperator() != null
                ? request.getConditionOperator()
                : RuleConditionOperator.AND;
        return new RuleConditionsNormalized(operator, conditions);
    }

    private CategorizationRuleCondition toCondition(CategorizationRuleConditionRequest request) {
        return CategorizationRuleCondition.builder()
                .field(request.getField())
                .patternType(request.getPatternType())
                .value(request.getValue() != null ? request.getValue().trim() : null)
                .build();
    }

    private void validateCondition(CategorizationRuleCondition condition) {
        if (condition.getValue() == null || condition.getValue().isBlank()) {
            throw ApiException.badRequest("Condition value is required");
        }

        switch (condition.getField()) {
            case DESCRIPTION, PAYEE, MEMO -> validateTextCondition(condition);
            case ACCOUNT -> validateAccountCondition(condition);
            case AMOUNT -> validateAmountCondition(condition);
        }
    }

    private void validateTextCondition(CategorizationRuleCondition condition) {
        if (!TEXT_PATTERN_TYPES.contains(condition.getPatternType())) {
            throw ApiException.badRequest("Invalid pattern type for text condition");
        }
    }

    private void validateAccountCondition(CategorizationRuleCondition condition) {
        if (!ACCOUNT_PATTERN_TYPES.contains(condition.getPatternType())) {
            throw ApiException.badRequest("Account conditions support only exact matches");
        }

        try {
            Long.parseLong(condition.getValue());
        } catch (NumberFormatException exception) {
            throw ApiException.badRequest("Account condition value must be a numeric account id");
        }
    }

    private void validateAmountCondition(CategorizationRuleCondition condition) {
        if (!AMOUNT_PATTERN_TYPES.contains(condition.getPatternType())) {
            throw ApiException.badRequest("Invalid pattern type for amount condition");
        }

        try {
            new BigDecimal(condition.getValue());
        } catch (NumberFormatException exception) {
            throw ApiException.badRequest("Amount condition value must be numeric");
        }
    }

    private List<CategorizationRuleCondition> getEffectiveConditions(CategorizationRule rule) {
        if (rule.getConditions() != null && !rule.getConditions().isEmpty()) {
            return rule.getConditions();
        }

        if (rule.getMatchField() == null || rule.getPatternType() == null || rule.getPattern() == null) {
            return List.of();
        }

        return List.of(CategorizationRuleCondition.builder()
                .field(rule.getMatchField())
                .patternType(rule.getPatternType())
                .value(rule.getPattern())
                .build());
    }

    private record RuleConditionsNormalized(
            RuleConditionOperator operator,
            List<CategorizationRuleCondition> conditions
    ) {}
}
