package com.peter.budget.service;

import com.peter.budget.exception.ApiException;
import com.peter.budget.model.dto.CategorizationRuleConditionRequest;
import com.peter.budget.model.dto.CategorizationRuleUpsertRequest;
import com.peter.budget.model.entity.CategorizationRule;
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
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategorizationRuleServiceTest {

    private static final long USER_ID = 7L;
    private static final long CATEGORY_ID = 22L;
    private static final long RULE_ID = 123L;

    @Mock
    private CategorizationRuleRepository categorizationRuleRepository;
    @Mock
    private CategoryViewService categoryViewService;
    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private CategorizationRuleService categorizationRuleService;

    @Test
    void createRuleTriggersBackfillForExistingTransactions() {
        CategorizationRuleUpsertRequest request = baseRequest();

        when(categoryViewService.getEffectiveCategoryByIdForUser(USER_ID, CATEGORY_ID))
                .thenReturn(Optional.of(Category.builder().id(CATEGORY_ID).build()));
        when(categorizationRuleRepository.save(any(CategorizationRule.class)))
                .thenAnswer(invocation -> {
                    CategorizationRule saved = invocation.getArgument(0);
                    saved.setId(RULE_ID);
                    return saved;
                });

        var result = categorizationRuleService.createRule(USER_ID, request);

        assertEquals(RULE_ID, result.getId());
        assertEquals("ChatGPT", result.getName());
        assertEquals("openai", result.getPattern());
        verify(transactionService).backfillCategorizationRules(USER_ID);
    }

    @Test
    void updateRuleTriggersBackfillForExistingTransactions() {
        CategorizationRule existingRule = CategorizationRule.builder()
                .id(RULE_ID)
                .userId(USER_ID)
                .name("Old")
                .pattern("old")
                .patternType(PatternType.CONTAINS)
                .matchField(MatchField.DESCRIPTION)
                .categoryId(CATEGORY_ID)
                .priority(0)
                .active(true)
                .system(false)
                .build();

        CategorizationRuleUpsertRequest request = baseRequest();

        when(categorizationRuleRepository.findByIdAndUserId(RULE_ID, USER_ID))
                .thenReturn(Optional.of(existingRule));
        when(categoryViewService.getEffectiveCategoryByIdForUser(USER_ID, CATEGORY_ID))
                .thenReturn(Optional.of(Category.builder().id(CATEGORY_ID).build()));
        when(categorizationRuleRepository.save(existingRule)).thenReturn(existingRule);

        categorizationRuleService.updateRule(USER_ID, RULE_ID, request);

        verify(transactionService).backfillCategorizationRules(USER_ID);
    }

    @Test
    void createRuleSupportsConditionChains() {
        CategorizationRuleUpsertRequest request = CategorizationRuleUpsertRequest.builder()
                .name("Investments")
                .conditionOperator(RuleConditionOperator.AND)
                .conditions(List.of(
                        CategorizationRuleConditionRequest.builder()
                                .field(MatchField.DESCRIPTION)
                                .patternType(PatternType.CONTAINS)
                                .value("BROKERAGE")
                                .build(),
                        CategorizationRuleConditionRequest.builder()
                                .field(MatchField.ACCOUNT)
                                .patternType(PatternType.EQUALS)
                                .value("77")
                                .build(),
                        CategorizationRuleConditionRequest.builder()
                                .field(MatchField.AMOUNT)
                                .patternType(PatternType.EQUALS)
                                .value("-250.00")
                                .build()
                ))
                .categoryId(CATEGORY_ID)
                .priority(1)
                .active(true)
                .build();

        when(categoryViewService.getEffectiveCategoryByIdForUser(USER_ID, CATEGORY_ID))
                .thenReturn(Optional.of(Category.builder().id(CATEGORY_ID).build()));
        when(categorizationRuleRepository.save(any(CategorizationRule.class)))
                .thenAnswer(invocation -> {
                    CategorizationRule saved = invocation.getArgument(0);
                    saved.setId(RULE_ID);
                    return saved;
                });

        var result = categorizationRuleService.createRule(USER_ID, request);

        assertEquals(3, result.getConditions().size());
        assertEquals(RuleConditionOperator.AND, result.getConditionOperator());
    }

    // --- getRulesForUser tests ---

    @Test
    void getRulesForUserReturnsAllRules() {
        CategorizationRule rule1 = CategorizationRule.builder()
                .id(1L).userId(USER_ID).name("Rule 1")
                .pattern("coffee").patternType(PatternType.CONTAINS).matchField(MatchField.DESCRIPTION)
                .categoryId(CATEGORY_ID).priority(0).active(true).system(false).build();
        CategorizationRule rule2 = CategorizationRule.builder()
                .id(2L).userId(USER_ID).name("Rule 2")
                .pattern("uber").patternType(PatternType.CONTAINS).matchField(MatchField.DESCRIPTION)
                .categoryId(CATEGORY_ID).priority(1).active(true).system(false).build();

        when(categorizationRuleRepository.findByUserId(USER_ID)).thenReturn(List.of(rule1, rule2));

        var result = categorizationRuleService.getRulesForUser(USER_ID);

        assertEquals(2, result.size());
        assertEquals("Rule 1", result.get(0).getName());
        assertEquals("Rule 2", result.get(1).getName());
    }

    @Test
    void getRulesForUserReturnsEmptyListWhenNoRules() {
        when(categorizationRuleRepository.findByUserId(USER_ID)).thenReturn(List.of());

        var result = categorizationRuleService.getRulesForUser(USER_ID);

        assertTrue(result.isEmpty());
    }

    // --- deleteRule tests ---

    @Test
    void deleteRuleDeletesNonSystemRule() {
        CategorizationRule rule = CategorizationRule.builder()
                .id(RULE_ID).userId(USER_ID).name("Test Rule")
                .pattern("test").patternType(PatternType.CONTAINS).matchField(MatchField.DESCRIPTION)
                .categoryId(CATEGORY_ID).priority(0).active(true).system(false).build();

        when(categorizationRuleRepository.findByIdAndUserId(RULE_ID, USER_ID))
                .thenReturn(Optional.of(rule));

        categorizationRuleService.deleteRule(USER_ID, RULE_ID);

        verify(categorizationRuleRepository).deleteById(RULE_ID);
    }

    @Test
    void deleteRuleThrowsForbiddenForSystemRule() {
        CategorizationRule rule = CategorizationRule.builder()
                .id(RULE_ID).userId(USER_ID).name("System Rule")
                .pattern("test").patternType(PatternType.CONTAINS).matchField(MatchField.DESCRIPTION)
                .categoryId(CATEGORY_ID).priority(0).active(true).system(true).build();

        when(categorizationRuleRepository.findByIdAndUserId(RULE_ID, USER_ID))
                .thenReturn(Optional.of(rule));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> categorizationRuleService.deleteRule(USER_ID, RULE_ID)
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        verify(categorizationRuleRepository, never()).deleteById(any());
    }

    @Test
    void deleteRuleThrowsNotFoundForMissingRule() {
        when(categorizationRuleRepository.findByIdAndUserId(RULE_ID, USER_ID))
                .thenReturn(Optional.empty());

        ApiException exception = assertThrows(
                ApiException.class,
                () -> categorizationRuleService.deleteRule(USER_ID, RULE_ID)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    // --- updateRule edge cases ---

    @Test
    void updateRuleThrowsNotFoundWhenRuleMissing() {
        when(categorizationRuleRepository.findByIdAndUserId(RULE_ID, USER_ID))
                .thenReturn(Optional.empty());

        ApiException exception = assertThrows(
                ApiException.class,
                () -> categorizationRuleService.updateRule(USER_ID, RULE_ID, baseRequest())
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    void updateRuleThrowsForbiddenForSystemRule() {
        CategorizationRule systemRule = CategorizationRule.builder()
                .id(RULE_ID).userId(USER_ID).name("System Rule")
                .pattern("test").patternType(PatternType.CONTAINS).matchField(MatchField.DESCRIPTION)
                .categoryId(CATEGORY_ID).priority(0).active(true).system(true).build();

        when(categorizationRuleRepository.findByIdAndUserId(RULE_ID, USER_ID))
                .thenReturn(Optional.of(systemRule));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> categorizationRuleService.updateRule(USER_ID, RULE_ID, baseRequest())
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
    }

    // --- createRule validation tests ---

    @Test
    void createRuleThrowsNotFoundWhenCategoryNotAccessible() {
        CategorizationRuleUpsertRequest request = baseRequest();

        when(categoryViewService.getEffectiveCategoryByIdForUser(USER_ID, CATEGORY_ID))
                .thenReturn(Optional.empty());

        ApiException exception = assertThrows(
                ApiException.class,
                () -> categorizationRuleService.createRule(USER_ID, request)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    void createRuleThrowsBadRequestWhenNoConditionsAndNoLegacyFields() {
        CategorizationRuleUpsertRequest request = CategorizationRuleUpsertRequest.builder()
                .name("Bad Rule")
                .categoryId(CATEGORY_ID)
                .build();

        when(categoryViewService.getEffectiveCategoryByIdForUser(USER_ID, CATEGORY_ID))
                .thenReturn(Optional.of(Category.builder().id(CATEGORY_ID).build()));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> categorizationRuleService.createRule(USER_ID, request)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void createRuleThrowsBadRequestForInvalidAmountConditionValue() {
        CategorizationRuleUpsertRequest request = CategorizationRuleUpsertRequest.builder()
                .name("Bad Amount")
                .conditions(List.of(
                        CategorizationRuleConditionRequest.builder()
                                .field(MatchField.AMOUNT)
                                .patternType(PatternType.GREATER_THAN)
                                .value("not-a-number")
                                .build()
                ))
                .categoryId(CATEGORY_ID)
                .build();

        when(categoryViewService.getEffectiveCategoryByIdForUser(USER_ID, CATEGORY_ID))
                .thenReturn(Optional.of(Category.builder().id(CATEGORY_ID).build()));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> categorizationRuleService.createRule(USER_ID, request)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertTrue(exception.getMessage().contains("numeric"));
    }

    @Test
    void createRuleThrowsBadRequestForInvalidAccountConditionValue() {
        CategorizationRuleUpsertRequest request = CategorizationRuleUpsertRequest.builder()
                .name("Bad Account")
                .conditions(List.of(
                        CategorizationRuleConditionRequest.builder()
                                .field(MatchField.ACCOUNT)
                                .patternType(PatternType.EXACT)
                                .value("not-a-number")
                                .build()
                ))
                .categoryId(CATEGORY_ID)
                .build();

        when(categoryViewService.getEffectiveCategoryByIdForUser(USER_ID, CATEGORY_ID))
                .thenReturn(Optional.of(Category.builder().id(CATEGORY_ID).build()));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> categorizationRuleService.createRule(USER_ID, request)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertTrue(exception.getMessage().contains("numeric account id"));
    }

    @Test
    void createRuleThrowsBadRequestForInvalidPatternTypeOnTextCondition() {
        CategorizationRuleUpsertRequest request = CategorizationRuleUpsertRequest.builder()
                .name("Bad Pattern")
                .conditions(List.of(
                        CategorizationRuleConditionRequest.builder()
                                .field(MatchField.DESCRIPTION)
                                .patternType(PatternType.GREATER_THAN)
                                .value("test")
                                .build()
                ))
                .categoryId(CATEGORY_ID)
                .build();

        when(categoryViewService.getEffectiveCategoryByIdForUser(USER_ID, CATEGORY_ID))
                .thenReturn(Optional.of(Category.builder().id(CATEGORY_ID).build()));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> categorizationRuleService.createRule(USER_ID, request)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertTrue(exception.getMessage().contains("Invalid pattern type"));
    }

    @Test
    void createRuleThrowsBadRequestForAccountConditionWithContainsPattern() {
        CategorizationRuleUpsertRequest request = CategorizationRuleUpsertRequest.builder()
                .name("Bad Account Pattern")
                .conditions(List.of(
                        CategorizationRuleConditionRequest.builder()
                                .field(MatchField.ACCOUNT)
                                .patternType(PatternType.CONTAINS)
                                .value("123")
                                .build()
                ))
                .categoryId(CATEGORY_ID)
                .build();

        when(categoryViewService.getEffectiveCategoryByIdForUser(USER_ID, CATEGORY_ID))
                .thenReturn(Optional.of(Category.builder().id(CATEGORY_ID).build()));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> categorizationRuleService.createRule(USER_ID, request)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertTrue(exception.getMessage().contains("exact matches"));
    }

    @Test
    void createRuleThrowsBadRequestForEmptyConditionValue() {
        CategorizationRuleUpsertRequest request = CategorizationRuleUpsertRequest.builder()
                .name("Empty Value")
                .conditions(List.of(
                        CategorizationRuleConditionRequest.builder()
                                .field(MatchField.DESCRIPTION)
                                .patternType(PatternType.CONTAINS)
                                .value("   ")
                                .build()
                ))
                .categoryId(CATEGORY_ID)
                .build();

        when(categoryViewService.getEffectiveCategoryByIdForUser(USER_ID, CATEGORY_ID))
                .thenReturn(Optional.of(Category.builder().id(CATEGORY_ID).build()));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> categorizationRuleService.createRule(USER_ID, request)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertTrue(exception.getMessage().contains("value is required"));
    }

    @Test
    void createRuleWithOrConditionOperator() {
        CategorizationRuleUpsertRequest request = CategorizationRuleUpsertRequest.builder()
                .name("OR Rule")
                .conditionOperator(RuleConditionOperator.OR)
                .conditions(List.of(
                        CategorizationRuleConditionRequest.builder()
                                .field(MatchField.DESCRIPTION)
                                .patternType(PatternType.CONTAINS)
                                .value("uber")
                                .build(),
                        CategorizationRuleConditionRequest.builder()
                                .field(MatchField.DESCRIPTION)
                                .patternType(PatternType.CONTAINS)
                                .value("lyft")
                                .build()
                ))
                .categoryId(CATEGORY_ID)
                .build();

        when(categoryViewService.getEffectiveCategoryByIdForUser(USER_ID, CATEGORY_ID))
                .thenReturn(Optional.of(Category.builder().id(CATEGORY_ID).build()));
        when(categorizationRuleRepository.save(any(CategorizationRule.class)))
                .thenAnswer(invocation -> {
                    CategorizationRule saved = invocation.getArgument(0);
                    saved.setId(RULE_ID);
                    return saved;
                });

        var result = categorizationRuleService.createRule(USER_ID, request);

        assertEquals(RuleConditionOperator.OR, result.getConditionOperator());
        assertEquals(2, result.getConditions().size());
    }

    private CategorizationRuleUpsertRequest baseRequest() {
        return CategorizationRuleUpsertRequest.builder()
                .name("  ChatGPT  ")
                .pattern("  openai  ")
                .patternType(PatternType.CONTAINS)
                .matchField(MatchField.DESCRIPTION)
                .categoryId(CATEGORY_ID)
                .priority(1)
                .active(true)
                .build();
    }
}
