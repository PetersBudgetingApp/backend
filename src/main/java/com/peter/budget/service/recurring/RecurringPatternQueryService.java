package com.peter.budget.service.recurring;

import com.peter.budget.exception.ApiException;
import com.peter.budget.model.dto.CategoryDto;
import com.peter.budget.model.dto.RecurringPatternDto;
import com.peter.budget.model.dto.UpcomingBillDto;
import com.peter.budget.model.entity.Category;
import com.peter.budget.model.entity.RecurringPattern;
import com.peter.budget.repository.CategoryRepository;
import com.peter.budget.repository.RecurringPatternRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecurringPatternQueryService {

    private final RecurringPatternRepository patternRepository;
    private final CategoryRepository categoryRepository;

    public List<RecurringPatternDto> getRecurringPatterns(Long userId) {
        return patternRepository.findActiveByUserId(userId).stream()
                .map(this::toDto)
                .toList();
    }

    public List<UpcomingBillDto> getUpcomingBills(Long userId, int days) {
        LocalDate now = LocalDate.now();
        LocalDate endDate = now.plusDays(days);

        return patternRepository.findUpcomingBills(userId, now.minusDays(7), endDate).stream()
                .map(pattern -> toUpcomingBillDto(pattern, now))
                .sorted(Comparator.comparing(UpcomingBillDto::getDueDate,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    public List<UpcomingBillDto> getBillsForMonth(Long userId, int year, int month) {
        LocalDate now = LocalDate.now();

        return patternRepository.findBillsForMonth(userId, year, month).stream()
                .map(pattern -> toUpcomingBillDto(pattern, now))
                .sorted(Comparator.comparing(UpcomingBillDto::getDueDate,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    @Transactional
    public void deletePattern(Long userId, Long patternId) {
        RecurringPattern pattern = patternRepository.findByIdAndUserId(patternId, userId)
                .orElseThrow(() -> ApiException.notFound("Pattern not found"));
        patternRepository.deleteById(pattern.getId());
    }

    @Transactional
    public RecurringPatternDto togglePatternActive(Long userId, Long patternId, boolean active) {
        RecurringPattern pattern = patternRepository.findByIdAndUserId(patternId, userId)
                .orElseThrow(() -> ApiException.notFound("Pattern not found"));
        pattern.setActive(active);
        pattern = patternRepository.save(pattern);
        return toDto(pattern);
    }

    private UpcomingBillDto toUpcomingBillDto(RecurringPattern pattern, LocalDate now) {
        boolean overdue = pattern.getNextExpectedDate() != null &&
                pattern.getNextExpectedDate().isBefore(now);
        int daysUntilDue = pattern.getNextExpectedDate() != null
                ? (int) ChronoUnit.DAYS.between(now, pattern.getNextExpectedDate())
                : 0;

        CategoryDto categoryDto = null;
        if (pattern.getCategoryId() != null) {
            categoryDto = categoryRepository.findById(pattern.getCategoryId())
                    .map(this::toCategoryDto)
                    .orElse(null);
        }

        return UpcomingBillDto.builder()
                .patternId(pattern.getId())
                .name(pattern.getName())
                .expectedAmount(pattern.getExpectedAmount())
                .dueDate(pattern.getNextExpectedDate())
                .daysUntilDue(daysUntilDue)
                .category(categoryDto)
                .overdue(overdue)
                .build();
    }

    private RecurringPatternDto toDto(RecurringPattern pattern) {
        CategoryDto categoryDto = null;
        if (pattern.getCategoryId() != null) {
            categoryDto = categoryRepository.findById(pattern.getCategoryId())
                    .map(this::toCategoryDto)
                    .orElse(null);
        }

        return RecurringPatternDto.builder()
                .id(pattern.getId())
                .name(pattern.getName())
                .merchantPattern(pattern.getMerchantPattern())
                .expectedAmount(pattern.getExpectedAmount())
                .frequency(pattern.getFrequency())
                .dayOfMonth(pattern.getDayOfMonth())
                .nextExpectedDate(pattern.getNextExpectedDate())
                .category(categoryDto)
                .bill(pattern.isBill())
                .active(pattern.isActive())
                .lastOccurrenceAt(pattern.getLastOccurrenceAt())
                .build();
    }

    private CategoryDto toCategoryDto(Category category) {
        return CategoryDto.builder()
                .id(category.getId())
                .name(category.getName())
                .icon(category.getIcon())
                .color(category.getColor())
                .categoryType(category.getCategoryType())
                .build();
    }
}
