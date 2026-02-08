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

    // --- STARTS_WITH pattern tests ---

    @Test
    void categorizeMatchesStartsWithPattern() {
        CategorizationRule rule = rule(110L, VISIBLE_CATEGORY_ID, RuleConditionOperator.AND, List.of(
                condition(MatchField.DESCRIPTION, PatternType.STARTS_WITH, "STARBUCKS")
        ));

        when(categorizationRuleRepository.findActiveRulesForUser(USER_ID)).thenReturn(List.of(rule));
        when(categoryViewService.getEffectiveCategoriesForUser(USER_ID))
                .thenReturn(List.of(Category.builder().id(VISIBLE_CATEGORY_ID).build()));

        AutoCategorizationService.CategorizationMatch match = autoCategorizationService.categorize(
                USER_ID, ACCOUNT_ID, new BigDecimal("-5.00"), "STARBUCKS #1234 SEATTLE", null, null);

        assertNotNull(match);
        assertEquals(110L, match.ruleId());
    }

    @Test
    void categorizeRejectsStartsWithWhenTextStartsDifferently() {
        CategorizationRule rule = rule(111L, VISIBLE_CATEGORY_ID, RuleConditionOperator.AND, List.of(
                condition(MatchField.DESCRIPTION, PatternType.STARTS_WITH, "STARBUCKS")
        ));

        when(categorizationRuleRepository.findActiveRulesForUser(USER_ID)).thenReturn(List.of(rule));
        when(categoryViewService.getEffectiveCategoriesForUser(USER_ID))
                .thenReturn(List.of(Category.builder().id(VISIBLE_CATEGORY_ID).build()));

        AutoCategorizationService.CategorizationMatch match = autoCategorizationService.categorize(
                USER_ID, ACCOUNT_ID, new BigDecimal("-5.00"), "I LOVE STARBUCKS", null, null);

        assertNull(match);
    }

    // --- ENDS_WITH pattern tests ---

    @Test
    void categorizeMatchesEndsWithPattern() {
        CategorizationRule rule = rule(112L, VISIBLE_CATEGORY_ID, RuleConditionOperator.AND, List.of(
                condition(MatchField.DESCRIPTION, PatternType.ENDS_WITH, "SUBSCRIPTION")
        ));

        when(categorizationRuleRepository.findActiveRulesForUser(USER_ID)).thenReturn(List.of(rule));
        when(categoryViewService.getEffectiveCategoriesForUser(USER_ID))
                .thenReturn(List.of(Category.builder().id(VISIBLE_CATEGORY_ID).build()));

        AutoCategorizationService.CategorizationMatch match = autoCategorizationService.categorize(
                USER_ID, ACCOUNT_ID, new BigDecimal("-15.00"), "NETFLIX MONTHLY SUBSCRIPTION", null, null);

        assertNotNull(match);
        assertEquals(112L, match.ruleId());
    }

    // --- EXACT pattern tests ---

    @Test
    void categorizeMatchesExactPatternCaseInsensitive() {
        CategorizationRule rule = rule(113L, VISIBLE_CATEGORY_ID, RuleConditionOperator.AND, List.of(
                condition(MatchField.DESCRIPTION, PatternType.EXACT, "Netflix")
        ));

        when(categorizationRuleRepository.findActiveRulesForUser(USER_ID)).thenReturn(List.of(rule));
        when(categoryViewService.getEffectiveCategoriesForUser(USER_ID))
                .thenReturn(List.of(Category.builder().id(VISIBLE_CATEGORY_ID).build()));

        AutoCategorizationService.CategorizationMatch match = autoCategorizationService.categorize(
                USER_ID, ACCOUNT_ID, new BigDecimal("-15.00"), "NETFLIX", null, null);

        assertNotNull(match);
    }

    @Test
    void categorizeRejectsExactPatternWhenNotFullMatch() {
        CategorizationRule rule = rule(114L, VISIBLE_CATEGORY_ID, RuleConditionOperator.AND, List.of(
                condition(MatchField.DESCRIPTION, PatternType.EXACT, "Netflix")
        ));

        when(categorizationRuleRepository.findActiveRulesForUser(USER_ID)).thenReturn(List.of(rule));
        when(categoryViewService.getEffectiveCategoriesForUser(USER_ID))
                .thenReturn(List.of(Category.builder().id(VISIBLE_CATEGORY_ID).build()));

        AutoCategorizationService.CategorizationMatch match = autoCategorizationService.categorize(
                USER_ID, ACCOUNT_ID, new BigDecimal("-15.00"), "NETFLIX PREMIUM", null, null);

        assertNull(match);
    }

    // --- REGEX pattern tests ---

    @Test
    void categorizeMatchesRegexPattern() {
        CategorizationRule rule = rule(115L, VISIBLE_CATEGORY_ID, RuleConditionOperator.AND, List.of(
                condition(MatchField.DESCRIPTION, PatternType.REGEX, "UBER.*TRIP")
        ));

        when(categorizationRuleRepository.findActiveRulesForUser(USER_ID)).thenReturn(List.of(rule));
        when(categoryViewService.getEffectiveCategoriesForUser(USER_ID))
                .thenReturn(List.of(Category.builder().id(VISIBLE_CATEGORY_ID).build()));

        AutoCategorizationService.CategorizationMatch match = autoCategorizationService.categorize(
                USER_ID, ACCOUNT_ID, new BigDecimal("-25.00"), "UBER EATS TRIP #5678", null, null);

        assertNotNull(match);
    }

    @Test
    void categorizeHandlesInvalidRegexGracefully() {
        CategorizationRule rule = rule(116L, VISIBLE_CATEGORY_ID, RuleConditionOperator.AND, List.of(
                condition(MatchField.DESCRIPTION, PatternType.REGEX, "[invalid(regex")
        ));

        when(categorizationRuleRepository.findActiveRulesForUser(USER_ID)).thenReturn(List.of(rule));
        when(categoryViewService.getEffectiveCategoriesForUser(USER_ID))
                .thenReturn(List.of(Category.builder().id(VISIBLE_CATEGORY_ID).build()));

        AutoCategorizationService.CategorizationMatch match = autoCategorizationService.categorize(
                USER_ID, ACCOUNT_ID, new BigDecimal("-25.00"), "Some transaction", null, null);

        assertNull(match);
    }

    // --- PAYEE field tests ---

    @Test
    void categorizeMatchesPayeeField() {
        CategorizationRule rule = rule(117L, VISIBLE_CATEGORY_ID, RuleConditionOperator.AND, List.of(
                condition(MatchField.PAYEE, PatternType.CONTAINS, "AMAZON")
        ));

        when(categorizationRuleRepository.findActiveRulesForUser(USER_ID)).thenReturn(List.of(rule));
        when(categoryViewService.getEffectiveCategoriesForUser(USER_ID))
                .thenReturn(List.of(Category.builder().id(VISIBLE_CATEGORY_ID).build()));

        AutoCategorizationService.CategorizationMatch match = autoCategorizationService.categorize(
                USER_ID, ACCOUNT_ID, new BigDecimal("-50.00"), "Order #12345", "AMAZON.COM", null);

        assertNotNull(match);
        assertEquals(117L, match.ruleId());
    }

    // --- MEMO field tests ---

    @Test
    void categorizeMatchesMemoField() {
        CategorizationRule rule = rule(118L, VISIBLE_CATEGORY_ID, RuleConditionOperator.AND, List.of(
                condition(MatchField.MEMO, PatternType.CONTAINS, "GROCERY")
        ));

        when(categorizationRuleRepository.findActiveRulesForUser(USER_ID)).thenReturn(List.of(rule));
        when(categoryViewService.getEffectiveCategoriesForUser(USER_ID))
                .thenReturn(List.of(Category.builder().id(VISIBLE_CATEGORY_ID).build()));

        AutoCategorizationService.CategorizationMatch match = autoCategorizationService.categorize(
                USER_ID, ACCOUNT_ID, new BigDecimal("-100.00"), "COSTCO", null, "GROCERY PURCHASE #7890");

        assertNotNull(match);
    }

    // --- ACCOUNT field tests ---

    @Test
    void categorizeMatchesAccountField() {
        CategorizationRule rule = rule(119L, VISIBLE_CATEGORY_ID, RuleConditionOperator.AND, List.of(
                condition(MatchField.ACCOUNT, PatternType.EXACT, String.valueOf(ACCOUNT_ID))
        ));

        when(categorizationRuleRepository.findActiveRulesForUser(USER_ID)).thenReturn(List.of(rule));
        when(categoryViewService.getEffectiveCategoriesForUser(USER_ID))
                .thenReturn(List.of(Category.builder().id(VISIBLE_CATEGORY_ID).build()));

        AutoCategorizationService.CategorizationMatch match = autoCategorizationService.categorize(
                USER_ID, ACCOUNT_ID, new BigDecimal("-10.00"), "Something", null, null);

        assertNotNull(match);
    }

    @Test
    void categorizeRejectsAccountFieldWithNonExactPattern() {
        CategorizationRule rule = rule(120L, VISIBLE_CATEGORY_ID, RuleConditionOperator.AND, List.of(
                condition(MatchField.ACCOUNT, PatternType.CONTAINS, String.valueOf(ACCOUNT_ID))
        ));

        when(categorizationRuleRepository.findActiveRulesForUser(USER_ID)).thenReturn(List.of(rule));
        when(categoryViewService.getEffectiveCategoriesForUser(USER_ID))
                .thenReturn(List.of(Category.builder().id(VISIBLE_CATEGORY_ID).build()));

        AutoCategorizationService.CategorizationMatch match = autoCategorizationService.categorize(
                USER_ID, ACCOUNT_ID, new BigDecimal("-10.00"), "Something", null, null);

        assertNull(match);
    }

    @Test
    void categorizeRejectsAccountFieldWithInvalidNumber() {
        CategorizationRule rule = rule(121L, VISIBLE_CATEGORY_ID, RuleConditionOperator.AND, List.of(
                condition(MatchField.ACCOUNT, PatternType.EXACT, "not-a-number")
        ));

        when(categorizationRuleRepository.findActiveRulesForUser(USER_ID)).thenReturn(List.of(rule));
        when(categoryViewService.getEffectiveCategoriesForUser(USER_ID))
                .thenReturn(List.of(Category.builder().id(VISIBLE_CATEGORY_ID).build()));

        AutoCategorizationService.CategorizationMatch match = autoCategorizationService.categorize(
                USER_ID, ACCOUNT_ID, new BigDecimal("-10.00"), "Something", null, null);

        assertNull(match);
    }

    // --- AMOUNT field tests ---

    @Test
    void categorizeMatchesAmountGreaterThan() {
        CategorizationRule rule = rule(122L, VISIBLE_CATEGORY_ID, RuleConditionOperator.AND, List.of(
                condition(MatchField.AMOUNT, PatternType.GREATER_THAN, "100.00")
        ));

        when(categorizationRuleRepository.findActiveRulesForUser(USER_ID)).thenReturn(List.of(rule));
        when(categoryViewService.getEffectiveCategoriesForUser(USER_ID))
                .thenReturn(List.of(Category.builder().id(VISIBLE_CATEGORY_ID).build()));

        AutoCategorizationService.CategorizationMatch match = autoCategorizationService.categorize(
                USER_ID, ACCOUNT_ID, new BigDecimal("150.00"), "Deposit", null, null);

        assertNotNull(match);
    }

    @Test
    void categorizeMatchesAmountGreaterThanOrEqual() {
        CategorizationRule rule = rule(123L, VISIBLE_CATEGORY_ID, RuleConditionOperator.AND, List.of(
                condition(MatchField.AMOUNT, PatternType.GREATER_THAN_OR_EQUAL, "100.00")
        ));

        when(categorizationRuleRepository.findActiveRulesForUser(USER_ID)).thenReturn(List.of(rule));
        when(categoryViewService.getEffectiveCategoriesForUser(USER_ID))
                .thenReturn(List.of(Category.builder().id(VISIBLE_CATEGORY_ID).build()));

        AutoCategorizationService.CategorizationMatch match = autoCategorizationService.categorize(
                USER_ID, ACCOUNT_ID, new BigDecimal("100.00"), "Deposit", null, null);

        assertNotNull(match);
    }

    @Test
    void categorizeMatchesAmountLessThan() {
        CategorizationRule rule = rule(124L, VISIBLE_CATEGORY_ID, RuleConditionOperator.AND, List.of(
                condition(MatchField.AMOUNT, PatternType.LESS_THAN, "0")
        ));

        when(categorizationRuleRepository.findActiveRulesForUser(USER_ID)).thenReturn(List.of(rule));
        when(categoryViewService.getEffectiveCategoriesForUser(USER_ID))
                .thenReturn(List.of(Category.builder().id(VISIBLE_CATEGORY_ID).build()));

        AutoCategorizationService.CategorizationMatch match = autoCategorizationService.categorize(
                USER_ID, ACCOUNT_ID, new BigDecimal("-50.00"), "Purchase", null, null);

        assertNotNull(match);
    }

    @Test
    void categorizeMatchesAmountLessThanOrEqual() {
        CategorizationRule rule = rule(125L, VISIBLE_CATEGORY_ID, RuleConditionOperator.AND, List.of(
                condition(MatchField.AMOUNT, PatternType.LESS_THAN_OR_EQUAL, "-100.00")
        ));

        when(categorizationRuleRepository.findActiveRulesForUser(USER_ID)).thenReturn(List.of(rule));
        when(categoryViewService.getEffectiveCategoriesForUser(USER_ID))
                .thenReturn(List.of(Category.builder().id(VISIBLE_CATEGORY_ID).build()));

        AutoCategorizationService.CategorizationMatch match = autoCategorizationService.categorize(
                USER_ID, ACCOUNT_ID, new BigDecimal("-100.00"), "Big purchase", null, null);

        assertNotNull(match);
    }

    @Test
    void categorizeRejectsAmountWithInvalidNumber() {
        CategorizationRule rule = rule(126L, VISIBLE_CATEGORY_ID, RuleConditionOperator.AND, List.of(
                condition(MatchField.AMOUNT, PatternType.GREATER_THAN, "not-a-number")
        ));

        when(categorizationRuleRepository.findActiveRulesForUser(USER_ID)).thenReturn(List.of(rule));
        when(categoryViewService.getEffectiveCategoriesForUser(USER_ID))
                .thenReturn(List.of(Category.builder().id(VISIBLE_CATEGORY_ID).build()));

        AutoCategorizationService.CategorizationMatch match = autoCategorizationService.categorize(
                USER_ID, ACCOUNT_ID, new BigDecimal("-50.00"), "Purchase", null, null);

        assertNull(match);
    }

    // --- Null text field handling ---

    @Test
    void categorizeHandlesNullDescription() {
        CategorizationRule rule = rule(127L, VISIBLE_CATEGORY_ID, RuleConditionOperator.AND, List.of(
                condition(MatchField.DESCRIPTION, PatternType.CONTAINS, "TEST")
        ));

        when(categorizationRuleRepository.findActiveRulesForUser(USER_ID)).thenReturn(List.of(rule));
        when(categoryViewService.getEffectiveCategoriesForUser(USER_ID))
                .thenReturn(List.of(Category.builder().id(VISIBLE_CATEGORY_ID).build()));

        AutoCategorizationService.CategorizationMatch match = autoCategorizationService.categorize(
                USER_ID, ACCOUNT_ID, new BigDecimal("-10.00"), null, null, null);

        assertNull(match);
    }

    @Test
    void categorizeHandlesNullPayeeAndMemo() {
        CategorizationRule rule = rule(128L, VISIBLE_CATEGORY_ID, RuleConditionOperator.AND, List.of(
                condition(MatchField.PAYEE, PatternType.CONTAINS, "TEST")
        ));

        when(categorizationRuleRepository.findActiveRulesForUser(USER_ID)).thenReturn(List.of(rule));
        when(categoryViewService.getEffectiveCategoriesForUser(USER_ID))
                .thenReturn(List.of(Category.builder().id(VISIBLE_CATEGORY_ID).build()));

        AutoCategorizationService.CategorizationMatch match = autoCategorizationService.categorize(
                USER_ID, ACCOUNT_ID, new BigDecimal("-10.00"), "Something", null, null);

        assertNull(match);
    }

    // --- Legacy single-field fallback ---

    @Test
    void categorizeUsesLegacySingleFieldFallbackWhenConditionsListEmpty() {
        CategorizationRule legacyRule = CategorizationRule.builder()
                .id(130L)
                .userId(USER_ID)
                .name("Legacy rule")
                .pattern("WALMART")
                .patternType(PatternType.CONTAINS)
                .matchField(MatchField.DESCRIPTION)
                .conditionOperator(null)
                .conditions(List.of())
                .categoryId(VISIBLE_CATEGORY_ID)
                .priority(0)
                .active(true)
                .system(false)
                .build();

        when(categorizationRuleRepository.findActiveRulesForUser(USER_ID)).thenReturn(List.of(legacyRule));
        when(categoryViewService.getEffectiveCategoriesForUser(USER_ID))
                .thenReturn(List.of(Category.builder().id(VISIBLE_CATEGORY_ID).build()));

        AutoCategorizationService.CategorizationMatch match = autoCategorizationService.categorize(
                USER_ID, ACCOUNT_ID, new BigDecimal("-75.00"), "WALMART STORE #456", null, null);

        assertNotNull(match);
        assertEquals(130L, match.ruleId());
    }

    @Test
    void categorizeReturnsNullWhenNoRulesExist() {
        when(categorizationRuleRepository.findActiveRulesForUser(USER_ID)).thenReturn(List.of());
        when(categoryViewService.getEffectiveCategoriesForUser(USER_ID))
                .thenReturn(List.of(Category.builder().id(VISIBLE_CATEGORY_ID).build()));

        AutoCategorizationService.CategorizationMatch match = autoCategorizationService.categorize(
                USER_ID, ACCOUNT_ID, new BigDecimal("-10.00"), "Something", null, null);

        assertNull(match);
    }

    @Test
    void categorizeReturnsNullWhenConditionHasNullFields() {
        CategorizationRule rule = rule(131L, VISIBLE_CATEGORY_ID, RuleConditionOperator.AND, List.of(
                CategorizationRuleCondition.builder()
                        .field(null)
                        .patternType(null)
                        .value(null)
                        .build()
        ));

        when(categorizationRuleRepository.findActiveRulesForUser(USER_ID)).thenReturn(List.of(rule));
        when(categoryViewService.getEffectiveCategoriesForUser(USER_ID))
                .thenReturn(List.of(Category.builder().id(VISIBLE_CATEGORY_ID).build()));

        AutoCategorizationService.CategorizationMatch match = autoCategorizationService.categorize(
                USER_ID, ACCOUNT_ID, new BigDecimal("-10.00"), "Something", null, null);

        assertNull(match);
    }

    @Test
    void categorizeMatchesAmountExact() {
        CategorizationRule rule = rule(132L, VISIBLE_CATEGORY_ID, RuleConditionOperator.AND, List.of(
                condition(MatchField.AMOUNT, PatternType.EXACT, "-9.99")
        ));

        when(categorizationRuleRepository.findActiveRulesForUser(USER_ID)).thenReturn(List.of(rule));
        when(categoryViewService.getEffectiveCategoriesForUser(USER_ID))
                .thenReturn(List.of(Category.builder().id(VISIBLE_CATEGORY_ID).build()));

        AutoCategorizationService.CategorizationMatch match = autoCategorizationService.categorize(
                USER_ID, ACCOUNT_ID, new BigDecimal("-9.99"), "Purchase", null, null);

        assertNotNull(match);
    }

    @Test
    void categorizeRejectsAmountWithTextPatternType() {
        CategorizationRule rule = rule(133L, VISIBLE_CATEGORY_ID, RuleConditionOperator.AND, List.of(
                condition(MatchField.AMOUNT, PatternType.CONTAINS, "-50.00")
        ));

        when(categorizationRuleRepository.findActiveRulesForUser(USER_ID)).thenReturn(List.of(rule));
        when(categoryViewService.getEffectiveCategoriesForUser(USER_ID))
                .thenReturn(List.of(Category.builder().id(VISIBLE_CATEGORY_ID).build()));

        AutoCategorizationService.CategorizationMatch match = autoCategorizationService.categorize(
                USER_ID, ACCOUNT_ID, new BigDecimal("-50.00"), "Purchase", null, null);

        assertNull(match);
    }

    @Test
    void categorizeRejectsTextFieldWithAmountPatternType() {
        CategorizationRule rule = rule(134L, VISIBLE_CATEGORY_ID, RuleConditionOperator.AND, List.of(
                condition(MatchField.DESCRIPTION, PatternType.GREATER_THAN, "100")
        ));

        when(categorizationRuleRepository.findActiveRulesForUser(USER_ID)).thenReturn(List.of(rule));
        when(categoryViewService.getEffectiveCategoriesForUser(USER_ID))
                .thenReturn(List.of(Category.builder().id(VISIBLE_CATEGORY_ID).build()));

        AutoCategorizationService.CategorizationMatch match = autoCategorizationService.categorize(
                USER_ID, ACCOUNT_ID, new BigDecimal("-50.00"), "Some description", null, null);

        assertNull(match);
    }

    @Test
    void categorizeRejectsNullAccountId() {
        CategorizationRule rule = rule(135L, VISIBLE_CATEGORY_ID, RuleConditionOperator.AND, List.of(
                condition(MatchField.ACCOUNT, PatternType.EXACT, "21")
        ));

        when(categorizationRuleRepository.findActiveRulesForUser(USER_ID)).thenReturn(List.of(rule));
        when(categoryViewService.getEffectiveCategoriesForUser(USER_ID))
                .thenReturn(List.of(Category.builder().id(VISIBLE_CATEGORY_ID).build()));

        AutoCategorizationService.CategorizationMatch match = autoCategorizationService.categorize(
                USER_ID, null, new BigDecimal("-10.00"), "Something", null, null);

        assertNull(match);
    }

    @Test
    void categorizeRejectsNullAmount() {
        CategorizationRule rule = rule(136L, VISIBLE_CATEGORY_ID, RuleConditionOperator.AND, List.of(
                condition(MatchField.AMOUNT, PatternType.GREATER_THAN, "0")
        ));

        when(categorizationRuleRepository.findActiveRulesForUser(USER_ID)).thenReturn(List.of(rule));
        when(categoryViewService.getEffectiveCategoriesForUser(USER_ID))
                .thenReturn(List.of(Category.builder().id(VISIBLE_CATEGORY_ID).build()));

        AutoCategorizationService.CategorizationMatch match = autoCategorizationService.categorize(
                USER_ID, ACCOUNT_ID, null, "Something", null, null);

        assertNull(match);
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
