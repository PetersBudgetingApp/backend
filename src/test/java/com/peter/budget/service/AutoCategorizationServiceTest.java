package com.peter.budget.service;

import com.peter.budget.model.entity.CategorizationRule;
import com.peter.budget.model.entity.CategorizationRuleCondition;
import com.peter.budget.model.entity.Category;
import com.peter.budget.model.enums.MatchField;
import com.peter.budget.model.enums.PatternType;
import com.peter.budget.model.enums.RuleConditionOperator;
import com.peter.budget.repository.CategorizationRuleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutoCategorizationServiceTest {

    private static final long USER_ID = 7L;
    private static final long ACCOUNT_ID = 21L;
    private static final long HIDDEN_CATEGORY_ID = 19L;
    private static final long VISIBLE_CATEGORY_ID = 43L;

    @Mock
    private CategorizationRuleRepository categorizationRuleRepository;
    @Mock
    private CategoryViewService categoryViewService;

    @InjectMocks
    private AutoCategorizationService autoCategorizationService;

    @Test
    void categorizeSkipsRulesForHiddenCategoriesAndUsesVisibleRule() {
        CategorizationRule hiddenMatch = rule(100L, HIDDEN_CATEGORY_ID, RuleConditionOperator.AND, List.of(
                condition(MatchField.DESCRIPTION, PatternType.CONTAINS, "OPENAI")
        ));
        CategorizationRule visibleMatch = rule(101L, VISIBLE_CATEGORY_ID, RuleConditionOperator.AND, List.of(
                condition(MatchField.DESCRIPTION, PatternType.CONTAINS, "OPENAI")
        ));

        when(categorizationRuleRepository.findActiveRulesForUser(USER_ID))
                .thenReturn(List.of(hiddenMatch, visibleMatch));
        when(categoryViewService.getEffectiveCategoriesForUser(USER_ID))
                .thenReturn(List.of(Category.builder().id(VISIBLE_CATEGORY_ID).build()));

        AutoCategorizationService.CategorizationMatch match = autoCategorizationService.categorize(
                USER_ID,
                ACCOUNT_ID,
                new BigDecimal("-20.15"),
                "OPENAI *CHATGPT SUBSCR SAN FRANCISCO",
                null,
                null
        );

        assertNotNull(match);
        assertEquals(101L, match.ruleId());
        assertEquals(VISIBLE_CATEGORY_ID, match.categoryId());
    }

    @Test
    void categorizeSupportsAndChainingAcrossDescriptionAccountAndAmount() {
        CategorizationRule chainedRule = rule(102L, VISIBLE_CATEGORY_ID, RuleConditionOperator.AND, List.of(
                condition(MatchField.DESCRIPTION, PatternType.CONTAINS, "BROKERAGE"),
                condition(MatchField.ACCOUNT, PatternType.EQUALS, String.valueOf(ACCOUNT_ID)),
                condition(MatchField.AMOUNT, PatternType.EQUALS, "-250.00")
        ));

        when(categorizationRuleRepository.findActiveRulesForUser(USER_ID))
                .thenReturn(List.of(chainedRule));
        when(categoryViewService.getEffectiveCategoriesForUser(USER_ID))
                .thenReturn(List.of(Category.builder().id(VISIBLE_CATEGORY_ID).build()));

        AutoCategorizationService.CategorizationMatch match = autoCategorizationService.categorize(
                USER_ID,
                ACCOUNT_ID,
                new BigDecimal("-250.00"),
                "Monthly Brokerage Deposit",
                null,
                null
        );

        assertNotNull(match);
        assertEquals(102L, match.ruleId());
    }

    @Test
    void categorizeSupportsOrChaining() {
        CategorizationRule orRule = rule(103L, VISIBLE_CATEGORY_ID, RuleConditionOperator.OR, List.of(
                condition(MatchField.DESCRIPTION, PatternType.CONTAINS, "THIS WILL NOT MATCH"),
                condition(MatchField.AMOUNT, PatternType.LESS_THAN_OR_EQUAL, "-100.00")
        ));

        when(categorizationRuleRepository.findActiveRulesForUser(USER_ID))
                .thenReturn(List.of(orRule));
        when(categoryViewService.getEffectiveCategoriesForUser(USER_ID))
                .thenReturn(List.of(Category.builder().id(VISIBLE_CATEGORY_ID).build()));

        AutoCategorizationService.CategorizationMatch match = autoCategorizationService.categorize(
                USER_ID,
                ACCOUNT_ID,
                new BigDecimal("-125.00"),
                "Something else",
                null,
                null
        );

        assertNotNull(match);
        assertEquals(103L, match.ruleId());
    }

    @Test
    void categorizeReturnsNullWhenOnlyHiddenCategoryRuleMatches() {
        CategorizationRule hiddenMatch = rule(100L, HIDDEN_CATEGORY_ID, RuleConditionOperator.AND, List.of(
                condition(MatchField.DESCRIPTION, PatternType.CONTAINS, "UBER TRIP")
        ));

        when(categorizationRuleRepository.findActiveRulesForUser(USER_ID))
                .thenReturn(List.of(hiddenMatch));
        when(categoryViewService.getEffectiveCategoriesForUser(USER_ID))
                .thenReturn(List.of(Category.builder().id(VISIBLE_CATEGORY_ID).build()));

        AutoCategorizationService.CategorizationMatch match = autoCategorizationService.categorize(
                USER_ID,
                ACCOUNT_ID,
                new BigDecimal("-35.15"),
                "UBER TRIP HTTPS://HELP.UB",
                null,
                null
        );

        assertNull(match);
    }

    private CategorizationRuleCondition condition(MatchField field, PatternType patternType, String value) {
        return CategorizationRuleCondition.builder()
                .field(field)
                .patternType(patternType)
                .value(value)
                .build();
    }

    private CategorizationRule rule(
            Long ruleId,
            Long categoryId,
            RuleConditionOperator operator,
            List<CategorizationRuleCondition> conditions
    ) {
        CategorizationRuleCondition primary = conditions.get(0);

        return CategorizationRule.builder()
                .id(ruleId)
                .userId(USER_ID)
                .name("Rule " + ruleId)
                .pattern(primary.getValue())
                .patternType(primary.getPatternType())
                .matchField(primary.getField())
                .conditionOperator(operator)
                .conditions(conditions)
                .categoryId(categoryId)
                .priority(0)
                .active(true)
                .system(false)
                .build();
    }
}
