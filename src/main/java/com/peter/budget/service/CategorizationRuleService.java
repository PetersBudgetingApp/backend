package com.peter.budget.service;

import com.peter.budget.exception.ApiException;
import com.peter.budget.model.dto.CategorizationRuleDto;
import com.peter.budget.model.dto.CategorizationRuleUpsertRequest;
import com.peter.budget.model.entity.CategorizationRule;
import com.peter.budget.repository.CategorizationRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategorizationRuleService {

    private final CategorizationRuleRepository categorizationRuleRepository;
    private final CategoryViewService categoryViewService;

    public List<CategorizationRuleDto> getRulesForUser(Long userId) {
        return categorizationRuleRepository.findByUserId(userId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public CategorizationRuleDto createRule(Long userId, CategorizationRuleUpsertRequest request) {
        assertCategoryAccessible(userId, request.getCategoryId());

        CategorizationRule rule = CategorizationRule.builder()
                .userId(userId)
                .name(request.getName().trim())
                .pattern(request.getPattern().trim())
                .patternType(request.getPatternType())
                .matchField(request.getMatchField())
                .categoryId(request.getCategoryId())
                .priority(request.getPriority() != null ? request.getPriority() : 0)
                .active(request.getActive() == null || request.getActive())
                .system(false)
                .build();

        rule = categorizationRuleRepository.save(rule);
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

        rule.setName(request.getName().trim());
        rule.setPattern(request.getPattern().trim());
        rule.setPatternType(request.getPatternType());
        rule.setMatchField(request.getMatchField());
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
        return CategorizationRuleDto.builder()
                .id(rule.getId())
                .name(rule.getName())
                .pattern(rule.getPattern())
                .patternType(rule.getPatternType())
                .matchField(rule.getMatchField())
                .categoryId(rule.getCategoryId())
                .priority(rule.getPriority())
                .active(rule.isActive())
                .system(rule.isSystem())
                .build();
    }
}
