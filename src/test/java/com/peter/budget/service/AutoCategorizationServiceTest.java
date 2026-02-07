package com.peter.budget.service;

import com.peter.budget.model.entity.CategorizationRule;
import com.peter.budget.model.entity.Category;
import com.peter.budget.model.enums.MatchField;
import com.peter.budget.model.enums.PatternType;
import com.peter.budget.repository.CategorizationRuleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutoCategorizationServiceTest {

    private static final long USER_ID = 7L;
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
        CategorizationRule hiddenMatch = rule(100L, HIDDEN_CATEGORY_ID, "OPENAI");
        CategorizationRule visibleMatch = rule(101L, VISIBLE_CATEGORY_ID, "OPENAI");

        when(categorizationRuleRepository.findActiveRulesForUser(USER_ID))
                .thenReturn(List.of(hiddenMatch, visibleMatch));
        when(categoryViewService.getEffectiveCategoriesForUser(USER_ID))
                .thenReturn(List.of(Category.builder().id(VISIBLE_CATEGORY_ID).build()));

        AutoCategorizationService.CategorizationMatch match = autoCategorizationService.categorize(
                USER_ID,
                "OPENAI *CHATGPT SUBSCR SAN FRANCISCO",
                null,
                null
        );

        assertNotNull(match);
        assertEquals(101L, match.ruleId());
        assertEquals(VISIBLE_CATEGORY_ID, match.categoryId());
    }

    @Test
    void categorizeReturnsNullWhenOnlyHiddenCategoryRuleMatches() {
        CategorizationRule hiddenMatch = rule(100L, HIDDEN_CATEGORY_ID, "UBER TRIP");

        when(categorizationRuleRepository.findActiveRulesForUser(USER_ID))
                .thenReturn(List.of(hiddenMatch));
        when(categoryViewService.getEffectiveCategoriesForUser(USER_ID))
                .thenReturn(List.of(Category.builder().id(VISIBLE_CATEGORY_ID).build()));

        AutoCategorizationService.CategorizationMatch match = autoCategorizationService.categorize(
                USER_ID,
                "UBER TRIP HTTPS://HELP.UB",
                null,
                null
        );

        assertNull(match);
    }

    private CategorizationRule rule(Long ruleId, Long categoryId, String pattern) {
        return CategorizationRule.builder()
                .id(ruleId)
                .userId(USER_ID)
                .name("Rule " + ruleId)
                .pattern(pattern)
                .patternType(PatternType.CONTAINS)
                .matchField(MatchField.DESCRIPTION)
                .categoryId(categoryId)
                .priority(0)
                .active(true)
                .system(false)
                .build();
    }
}
