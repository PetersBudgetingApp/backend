package com.peter.budget.service;

import com.peter.budget.exception.ApiException;
import com.peter.budget.model.dto.BudgetMonthDto;
import com.peter.budget.model.dto.BudgetMonthUpsertRequest;
import com.peter.budget.model.dto.BudgetTargetDto;
import com.peter.budget.model.dto.BudgetTargetUpsertRequest;
import com.peter.budget.model.entity.Category;
import com.peter.budget.model.enums.CategoryType;
import com.peter.budget.repository.BudgetTargetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private static final String DEFAULT_CURRENCY = "USD";

    private final BudgetTargetRepository budgetTargetRepository;
    private final CategoryViewService categoryViewService;

    public BudgetMonthDto getBudgetMonth(Long userId, String monthRaw) {
        String month = normalizeMonth(monthRaw);
        return buildBudgetMonthDto(userId, month);
    }

    @Transactional
    public BudgetMonthDto upsertBudgetMonth(Long userId, String monthRaw, BudgetMonthUpsertRequest request) {
        String month = normalizeMonth(monthRaw);
        List<BudgetTargetUpsertRequest> requestTargets = request.getTargets() == null ? List.of() : request.getTargets();

        Map<Long, Category> categoriesById = categoryViewService.getEffectiveCategoryMapForUser(userId);
        Set<Long> uniqueCategoryIds = new HashSet<>();

        Map<Long, BudgetTargetRepository.UpsertBudgetTarget> requestedTargetsByCategory = new LinkedHashMap<>();
        for (BudgetTargetUpsertRequest target : requestTargets) {
            Long categoryId = target.getCategoryId();
            if (!uniqueCategoryIds.add(categoryId)) {
                throw ApiException.badRequest("Duplicate categoryId in targets: " + categoryId);
            }

            Category category = categoriesById.get(categoryId);
            if (category == null) {
                throw ApiException.badRequest("Category not found: " + categoryId);
            }
            if (category.getCategoryType() != CategoryType.EXPENSE
                    && category.getCategoryType() != CategoryType.UNCATEGORIZED) {
                throw ApiException.badRequest("Budgets can only be set on EXPENSE or UNCATEGORIZED categories");
            }

            BigDecimal normalizedAmount = target.getTargetAmount() == null
                    ? BigDecimal.ZERO
                    : target.getTargetAmount().max(BigDecimal.ZERO);
            String normalizedNotes = normalizedAmount.compareTo(BigDecimal.ZERO) > 0
                    ? normalizeNotes(target.getNotes())
                    : null;

            requestedTargetsByCategory.put(
                    categoryId,
                    new BudgetTargetRepository.UpsertBudgetTarget(categoryId, normalizedAmount, normalizedNotes)
            );
        }

        Map<Long, com.peter.budget.model.entity.BudgetTarget> currentTargetsByCategory = budgetTargetRepository
                .findLatestByUserIdAndMonthKey(userId, month)
                .stream()
                .collect(Collectors.toMap(com.peter.budget.model.entity.BudgetTarget::getCategoryId, target -> target));

        Set<Long> relevantCategoryIds = new HashSet<>(currentTargetsByCategory.keySet());
        relevantCategoryIds.addAll(requestedTargetsByCategory.keySet());

        List<BudgetTargetRepository.UpsertBudgetTarget> changedTargets = new ArrayList<>();
        for (Long categoryId : relevantCategoryIds.stream().sorted().toList()) {
            BudgetTargetRepository.UpsertBudgetTarget desiredTarget = desiredTargetForCategory(
                    categoryId,
                    requestedTargetsByCategory.get(categoryId)
            );
            if (matchesCurrentState(currentTargetsByCategory.get(categoryId), desiredTarget)) {
                continue;
            }
            changedTargets.add(desiredTarget);
        }

        if (!changedTargets.isEmpty()) {
            budgetTargetRepository.upsertMonthTargets(userId, month, changedTargets);
        }
        return getBudgetMonth(userId, month);
    }

    @Transactional
    public void deleteTarget(Long userId, String monthRaw, Long categoryId) {
        String month = normalizeMonth(monthRaw);
        budgetTargetRepository.findLatestByUserIdAndMonthKey(userId, month).stream()
                .filter(target -> Objects.equals(target.getCategoryId(), categoryId))
                .filter(target -> target.getTargetAmount() != null && target.getTargetAmount().compareTo(BigDecimal.ZERO) > 0)
                .findFirst()
                .ifPresent(target -> budgetTargetRepository.upsertMonthTargets(
                        userId,
                        month,
                        List.of(new BudgetTargetRepository.UpsertBudgetTarget(categoryId, BigDecimal.ZERO, null))
                ));
    }

    private BudgetMonthDto buildBudgetMonthDto(Long userId, String month) {
        List<BudgetTargetDto> targets = budgetTargetRepository.findEffectiveByUserIdAndMonthKey(userId, month).stream()
                .map(target -> BudgetTargetDto.builder()
                        .categoryId(target.getCategoryId())
                        .targetAmount(target.getTargetAmount())
                        .notes(target.getNotes())
                        .build())
                .toList();

        return BudgetMonthDto.builder()
                .month(month)
                .currency(DEFAULT_CURRENCY)
                .targets(targets)
                .hasChangesInMonth(budgetTargetRepository.existsByUserIdAndMonthKey(userId, month))
                .build();
    }

    private BudgetTargetRepository.UpsertBudgetTarget desiredTargetForCategory(
            Long categoryId,
            BudgetTargetRepository.UpsertBudgetTarget requestedTarget
    ) {
        if (requestedTarget == null || requestedTarget.targetAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return new BudgetTargetRepository.UpsertBudgetTarget(categoryId, BigDecimal.ZERO, null);
        }
        return requestedTarget;
    }

    private boolean matchesCurrentState(
            com.peter.budget.model.entity.BudgetTarget currentTarget,
            BudgetTargetRepository.UpsertBudgetTarget desiredTarget
    ) {
        BigDecimal currentAmount = currentTarget == null || currentTarget.getTargetAmount() == null
                ? BigDecimal.ZERO
                : currentTarget.getTargetAmount();
        String currentNotes = currentAmount.compareTo(BigDecimal.ZERO) > 0 ? normalizeNotes(currentTarget.getNotes()) : null;

        return currentAmount.compareTo(desiredTarget.targetAmount()) == 0
                && Objects.equals(currentNotes, desiredTarget.notes());
    }

    private String normalizeMonth(String monthRaw) {
        if (monthRaw == null || monthRaw.isBlank()) {
            throw ApiException.badRequest("month is required and must be in YYYY-MM format");
        }

        try {
            return YearMonth.parse(monthRaw).toString();
        } catch (DateTimeParseException ex) {
            throw ApiException.badRequest("month must be in YYYY-MM format");
        }
    }

    private String normalizeNotes(String notes) {
        if (notes == null) {
            return null;
        }
        String trimmed = notes.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
