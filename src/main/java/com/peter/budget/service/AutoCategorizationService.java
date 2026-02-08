package com.peter.budget.service;

import com.peter.budget.model.entity.CategorizationRule;
import com.peter.budget.model.entity.CategorizationRuleCondition;
import com.peter.budget.model.enums.PatternType;
import com.peter.budget.model.enums.RuleConditionOperator;
import com.peter.budget.repository.CategorizationRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AutoCategorizationService {

    private static final Set<PatternType> TEXT_PATTERN_TYPES = Set.of(
            PatternType.CONTAINS,
            PatternType.STARTS_WITH,
            PatternType.ENDS_WITH,
            PatternType.EXACT,
            PatternType.EQUALS,
            PatternType.REGEX
    );

    private final CategorizationRuleRepository ruleRepository;
    private final CategoryViewService categoryViewService;

    public CategorizationMatch categorize(
            Long userId,
            Long accountId,
            BigDecimal amount,
            String description,
            String payee,
            String memo
    ) {
        List<CategorizationRule> rules = ruleRepository.findActiveRulesForUser(userId);
        Set<Long> visibleCategoryIds = categoryViewService.getEffectiveCategoriesForUser(userId).stream()
                .map(category -> category.getId())
                .collect(Collectors.toSet());

        for (CategorizationRule rule : rules) {
            if (!visibleCategoryIds.contains(rule.getCategoryId())) {
                continue;
            }

            List<CategorizationRuleCondition> conditions = getEffectiveConditions(rule);
            boolean matched = matchesConditions(
                    conditions,
                    rule.getConditionOperator(),
                    accountId,
                    amount,
                    description,
                    payee,
                    memo
            );

            if (matched) {
                log.debug("Matched rule '{}' for account={} amount={}", rule.getName(), accountId, amount);
                return new CategorizationMatch(rule.getId(), rule.getCategoryId());
            }
        }

        return null;
    }

    public record CategorizationMatch(Long ruleId, Long categoryId) {}

    private boolean matchesConditions(
            List<CategorizationRuleCondition> conditions,
            RuleConditionOperator operator,
            Long accountId,
            BigDecimal amount,
            String description,
            String payee,
            String memo
    ) {
        if (conditions.isEmpty()) {
            return false;
        }

        RuleConditionOperator effectiveOperator = operator != null ? operator : RuleConditionOperator.AND;
        if (effectiveOperator == RuleConditionOperator.OR) {
            return conditions.stream().anyMatch(condition ->
                    matchesCondition(condition, accountId, amount, description, payee, memo));
        }

        return conditions.stream().allMatch(condition ->
                matchesCondition(condition, accountId, amount, description, payee, memo));
    }

    private boolean matchesCondition(
            CategorizationRuleCondition condition,
            Long accountId,
            BigDecimal amount,
            String description,
            String payee,
            String memo
    ) {
        if (condition.getField() == null || condition.getPatternType() == null || condition.getValue() == null) {
            return false;
        }

        return switch (condition.getField()) {
            case DESCRIPTION -> matchesText(description, condition.getValue(), condition.getPatternType());
            case PAYEE -> matchesText(payee, condition.getValue(), condition.getPatternType());
            case MEMO -> matchesText(memo, condition.getValue(), condition.getPatternType());
            case ACCOUNT -> matchesAccount(accountId, condition.getValue(), condition.getPatternType());
            case AMOUNT -> matchesAmount(amount, condition.getValue(), condition.getPatternType());
        };
    }

    private boolean matchesText(String text, String pattern, PatternType patternType) {
        if (text == null || pattern == null || !TEXT_PATTERN_TYPES.contains(patternType)) {
            return false;
        }

        String upperText = text.toUpperCase();
        String upperPattern = pattern.toUpperCase();

        return switch (patternType) {
            case CONTAINS -> upperText.contains(upperPattern);
            case STARTS_WITH -> upperText.startsWith(upperPattern);
            case ENDS_WITH -> upperText.endsWith(upperPattern);
            case EXACT, EQUALS -> upperText.equals(upperPattern);
            case REGEX -> matchesRegex(text, pattern);
            case GREATER_THAN, GREATER_THAN_OR_EQUAL, LESS_THAN, LESS_THAN_OR_EQUAL -> false;
        };
    }

    private boolean matchesAccount(Long accountId, String value, PatternType patternType) {
        if (accountId == null || value == null) {
            return false;
        }

        if (!(patternType == PatternType.EXACT || patternType == PatternType.EQUALS)) {
            return false;
        }

        try {
            return accountId.equals(Long.parseLong(value.trim()));
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private boolean matchesAmount(BigDecimal amount, String value, PatternType patternType) {
        if (amount == null || value == null) {
            return false;
        }

        BigDecimal expected;
        try {
            expected = new BigDecimal(value.trim());
        } catch (NumberFormatException exception) {
            return false;
        }

        int compare = amount.compareTo(expected);
        return switch (patternType) {
            case EXACT, EQUALS -> compare == 0;
            case GREATER_THAN -> compare > 0;
            case GREATER_THAN_OR_EQUAL -> compare >= 0;
            case LESS_THAN -> compare < 0;
            case LESS_THAN_OR_EQUAL -> compare <= 0;
            case CONTAINS, STARTS_WITH, ENDS_WITH, REGEX -> false;
        };
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

    private boolean matchesRegex(String text, String pattern) {
        try {
            return Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(text).find();
        } catch (PatternSyntaxException exception) {
            log.warn("Invalid regex pattern: {}", pattern);
            return false;
        }
    }
}
