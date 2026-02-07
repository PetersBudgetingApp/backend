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

import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private static final String DEFAULT_CURRENCY = "USD";

    private final BudgetTargetRepository budgetTargetRepository;
    private final CategoryViewService categoryViewService;

    public BudgetMonthDto getBudgetMonth(Long userId, String monthRaw) {
        String month = normalizeMonth(monthRaw);

        List<BudgetTargetDto> targets = budgetTargetRepository.findByUserIdAndMonthKey(userId, month).stream()
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
                .build();
    }

    @Transactional
    public BudgetMonthDto upsertBudgetMonth(Long userId, String monthRaw, BudgetMonthUpsertRequest request) {
        String month = normalizeMonth(monthRaw);
        List<BudgetTargetUpsertRequest> requestTargets = request.getTargets() == null ? List.of() : request.getTargets();

        Map<Long, Category> categoriesById = categoryViewService.getEffectiveCategoryMapForUser(userId);
        Set<Long> uniqueCategoryIds = new HashSet<>();

        List<BudgetTargetRepository.UpsertBudgetTarget> targets = requestTargets.stream()
                .map(target -> {
                    Long categoryId = target.getCategoryId();
                    if (!uniqueCategoryIds.add(categoryId)) {
                        throw ApiException.badRequest("Duplicate categoryId in targets: " + categoryId);
                    }

                    Category category = categoriesById.get(categoryId);
                    if (category == null) {
                        throw ApiException.badRequest("Category not found: " + categoryId);
                    }
                    if (category.getCategoryType() != CategoryType.EXPENSE) {
                        throw ApiException.badRequest("Budgets can only be set on EXPENSE categories");
                    }

                    String normalizedNotes = normalizeNotes(target.getNotes());
                    return new BudgetTargetRepository.UpsertBudgetTarget(
                            categoryId,
                            target.getTargetAmount(),
                            normalizedNotes
                    );
                })
                .toList();

        budgetTargetRepository.replaceMonthTargets(userId, month, targets);
        return getBudgetMonth(userId, month);
    }

    @Transactional
    public void deleteTarget(Long userId, String monthRaw, Long categoryId) {
        String month = normalizeMonth(monthRaw);
        budgetTargetRepository.deleteByUserIdAndMonthKeyAndCategoryId(userId, month, categoryId);
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
