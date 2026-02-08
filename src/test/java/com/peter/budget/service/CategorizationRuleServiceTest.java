package com.peter.budget.service;

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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
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
