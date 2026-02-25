package com.peter.budget.service;

import com.peter.budget.model.dto.MigrationImportRequest;
import com.peter.budget.model.dto.MigrationImportRequest.SnapshotAccount;
import com.peter.budget.model.dto.MigrationImportRequest.SnapshotBudgetMonth;
import com.peter.budget.model.dto.MigrationImportRequest.SnapshotCategory;
import com.peter.budget.model.dto.MigrationImportRequest.SnapshotRecurringPattern;
import com.peter.budget.model.dto.MigrationImportRequest.SnapshotRule;
import com.peter.budget.model.dto.MigrationImportRequest.SnapshotTransaction;
import com.peter.budget.model.dto.MigrationImportRequest.SnapshotTransferPair;
import com.peter.budget.model.dto.MigrationImportResponse;
import com.peter.budget.model.entity.Account;
import com.peter.budget.model.entity.BudgetTarget;
import com.peter.budget.model.entity.CategorizationRule;
import com.peter.budget.model.entity.CategorizationRuleCondition;
import com.peter.budget.model.entity.Category;
import com.peter.budget.model.entity.RecurringPattern;
import com.peter.budget.model.entity.Transaction;
import com.peter.budget.model.enums.AccountType;
import com.peter.budget.model.enums.CategoryType;
import com.peter.budget.model.enums.RuleConditionOperator;
import com.peter.budget.repository.AccountRepository;
import com.peter.budget.repository.BudgetTargetRepository;
import com.peter.budget.repository.CategorizationRuleRepository;
import com.peter.budget.repository.CategoryRepository;
import com.peter.budget.repository.RecurringPatternRepository;
import com.peter.budget.repository.TransactionWriteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class MigrationService {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final CategoryRepository categoryRepository;
    private final AccountRepository accountRepository;
    private final TransactionWriteRepository transactionWriteRepository;
    private final CategorizationRuleRepository categorizationRuleRepository;
    private final BudgetTargetRepository budgetTargetRepository;
    private final RecurringPatternRepository recurringPatternRepository;
    private final UncategorizedCategoryService uncategorizedCategoryService;

    @Transactional
    public MigrationImportResponse importSnapshot(Long userId, MigrationImportRequest request) {
        log.info("Starting migration import for user {}", userId);

        deleteAllUserData(userId);

        Long uncategorizedCategoryId = uncategorizedCategoryService.requireSystemUncategorizedCategoryId();

        Map<Long, Long> categoryIdMap = importCategories(userId, request.getCategories(), uncategorizedCategoryId);
        int categoriesImported = categoryIdMap.size();

        Map<Long, Long> accountIdMap = importAccounts(userId, request.getAccounts());
        int accountsImported = accountIdMap.size();

        Map<Long, Long> transactionIdMap = importTransactions(userId, request.getTransactions(),
                accountIdMap, categoryIdMap, uncategorizedCategoryId);
        int transactionsImported = transactionIdMap.size();

        relinkTransferPairs(request.getTransferPairs(), transactionIdMap);

        int rulesImported = importRules(userId, request.getRules(), categoryIdMap, uncategorizedCategoryId);

        int budgetsImported = importBudgets(userId, request.getBudgets(), categoryIdMap);

        int recurringImported = importRecurringPatterns(userId, request.getRecurringPatterns(), categoryIdMap);

        log.info("Migration import complete for user {}: {} accounts, {} categories, {} transactions, {} rules, {} budgets, {} recurring",
                userId, accountsImported, categoriesImported, transactionsImported, rulesImported, budgetsImported, recurringImported);

        return MigrationImportResponse.builder()
                .accountsImported(accountsImported)
                .categoriesImported(categoriesImported)
                .transactionsImported(transactionsImported)
                .rulesImported(rulesImported)
                .budgetsImported(budgetsImported)
                .recurringImported(recurringImported)
                .build();
    }

    private void deleteAllUserData(Long userId) {
        var params = new MapSqlParameterSource("userId", userId);

        // Delete in FK-safe order
        jdbcTemplate.update(
                "DELETE FROM transactions WHERE account_id IN (SELECT id FROM accounts WHERE user_id = :userId)",
                params);
        jdbcTemplate.update("DELETE FROM budget_targets WHERE user_id = :userId", params);
        jdbcTemplate.update("DELETE FROM categorization_rules WHERE user_id = :userId", params);
        jdbcTemplate.update("DELETE FROM recurring_patterns WHERE user_id = :userId", params);
        jdbcTemplate.update("DELETE FROM accounts WHERE user_id = :userId", params);
        jdbcTemplate.update("DELETE FROM category_overrides WHERE user_id = :userId", params);
        jdbcTemplate.update("DELETE FROM categories WHERE user_id = :userId", params);
    }

    private Map<Long, Long> importCategories(Long userId, List<SnapshotCategory> categories, Long uncategorizedCategoryId) {
        if (categories == null || categories.isEmpty()) {
            return Map.of();
        }

        Map<Long, Long> idMap = new HashMap<>();

        // Map system Uncategorized to the existing one
        for (SnapshotCategory cat : categories) {
            if (cat.getCategoryType() == CategoryType.UNCATEGORIZED || isUncategorizedByName(cat.getName())) {
                idMap.put(cat.getId(), uncategorizedCategoryId);
            }
        }

        // Separate parents (parentId == null or parent is Uncategorized-mapped) from children
        List<SnapshotCategory> roots = new ArrayList<>();
        List<SnapshotCategory> children = new ArrayList<>();
        for (SnapshotCategory cat : categories) {
            if (idMap.containsKey(cat.getId())) {
                continue; // skip Uncategorized
            }
            if (cat.getParentId() == null) {
                roots.add(cat);
            } else {
                children.add(cat);
            }
        }

        // Import root categories first
        for (SnapshotCategory cat : roots) {
            Category entity = Category.builder()
                    .userId(userId)
                    .parentId(null)
                    .name(cat.getName())
                    .icon(cat.getIcon())
                    .color(cat.getColor())
                    .categoryType(safeCategoryType(cat.getCategoryType()))
                    .system(false)
                    .sortOrder(0)
                    .build();
            entity = categoryRepository.save(entity);
            idMap.put(cat.getId(), entity.getId());
        }

        // Import children (may be multi-level, process iteratively)
        List<SnapshotCategory> remaining = new ArrayList<>(children);
        int maxPasses = 10;
        while (!remaining.isEmpty() && maxPasses-- > 0) {
            List<SnapshotCategory> deferred = new ArrayList<>();
            for (SnapshotCategory cat : remaining) {
                Long newParentId = idMap.get(cat.getParentId());
                if (newParentId == null) {
                    deferred.add(cat);
                    continue;
                }
                Category entity = Category.builder()
                        .userId(userId)
                        .parentId(newParentId)
                        .name(cat.getName())
                        .icon(cat.getIcon())
                        .color(cat.getColor())
                        .categoryType(safeCategoryType(cat.getCategoryType()))
                        .system(false)
                        .sortOrder(0)
                        .build();
                entity = categoryRepository.save(entity);
                idMap.put(cat.getId(), entity.getId());
            }
            remaining = deferred;
        }

        if (!remaining.isEmpty()) {
            log.warn("Could not resolve parent for {} categories during migration, importing as root", remaining.size());
            for (SnapshotCategory cat : remaining) {
                Category entity = Category.builder()
                        .userId(userId)
                        .parentId(null)
                        .name(cat.getName())
                        .icon(cat.getIcon())
                        .color(cat.getColor())
                        .categoryType(safeCategoryType(cat.getCategoryType()))
                        .system(false)
                        .sortOrder(0)
                        .build();
                entity = categoryRepository.save(entity);
                idMap.put(cat.getId(), entity.getId());
            }
        }

        return idMap;
    }

    private Map<Long, Long> importAccounts(Long userId, List<SnapshotAccount> accounts) {
        if (accounts == null || accounts.isEmpty()) {
            return Map.of();
        }

        Map<Long, Long> idMap = new HashMap<>();
        Instant now = Instant.now();

        for (SnapshotAccount acc : accounts) {
            Account entity = Account.builder()
                    .userId(userId)
                    .connectionId(null)
                    .externalId(null)
                    .name(acc.getName())
                    .institutionName(acc.getInstitutionName())
                    .accountType(acc.getAccountType() != null ? acc.getAccountType() : AccountType.OTHER)
                    .netWorthCategoryOverride(acc.getNetWorthCategory())
                    .currency(acc.getCurrency() != null ? acc.getCurrency() : "USD")
                    .currentBalance(acc.getCurrentBalance() != null ? acc.getCurrentBalance() : BigDecimal.ZERO)
                    .availableBalance(acc.getAvailableBalance())
                    .balanceUpdatedAt(parseInstantOrDefault(acc.getBalanceUpdatedAt(), now))
                    .active(acc.isActive())
                    .build();
            entity = accountRepository.save(entity);
            idMap.put(acc.getId(), entity.getId());
        }

        return idMap;
    }

    private Map<Long, Long> importTransactions(Long userId, List<SnapshotTransaction> transactions,
                                                Map<Long, Long> accountIdMap, Map<Long, Long> categoryIdMap,
                                                Long uncategorizedCategoryId) {
        if (transactions == null || transactions.isEmpty()) {
            return Map.of();
        }

        Map<Long, Long> idMap = new HashMap<>();

        for (SnapshotTransaction txn : transactions) {
            Long newAccountId = accountIdMap.get(txn.getAccountId());
            if (newAccountId == null) {
                log.warn("Skipping transaction {} - no mapped account for old accountId {}", txn.getId(), txn.getAccountId());
                continue;
            }

            Long newCategoryId = resolveCategoryId(txn.getCategory(), categoryIdMap, uncategorizedCategoryId);

            Transaction entity = Transaction.builder()
                    .accountId(newAccountId)
                    .externalId(null)
                    .postedAt(parseInstantOrDefault(txn.getPostedAt(), Instant.now()))
                    .transactedAt(parseInstantOrNull(txn.getTransactedAt()))
                    .amount(txn.getAmount() != null ? txn.getAmount() : BigDecimal.ZERO)
                    .pending(txn.isPending())
                    .description(txn.getDescription())
                    .payee(txn.getPayee())
                    .memo(txn.getMemo())
                    .categoryId(newCategoryId)
                    .categorizedByRuleId(null)
                    .manuallyCategorized(txn.isManuallyCategorized())
                    .transferPairId(null) // relinked later
                    .internalTransfer(txn.isInternalTransfer())
                    .excludeFromTotals(txn.isExcludeFromTotals())
                    .recurring(txn.isRecurring())
                    .recurringPatternId(null)
                    .notes(txn.getNotes())
                    .build();
            entity = transactionWriteRepository.save(entity);
            idMap.put(txn.getId(), entity.getId());
        }

        return idMap;
    }

    private void relinkTransferPairs(List<SnapshotTransferPair> transferPairs, Map<Long, Long> transactionIdMap) {
        if (transferPairs == null || transferPairs.isEmpty()) {
            return;
        }

        for (SnapshotTransferPair pair : transferPairs) {
            Long newFromId = transactionIdMap.get(pair.getFromTransactionId());
            Long newToId = transactionIdMap.get(pair.getToTransactionId());
            if (newFromId != null && newToId != null) {
                transactionWriteRepository.linkTransferPair(newFromId, newToId);
            }
        }
    }

    private int importRules(Long userId, List<SnapshotRule> rules, Map<Long, Long> categoryIdMap,
                            Long uncategorizedCategoryId) {
        if (rules == null || rules.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (SnapshotRule rule : rules) {
            Long newCategoryId = categoryIdMap.get(rule.getCategoryId());
            if (newCategoryId == null) {
                newCategoryId = uncategorizedCategoryId;
            }

            List<CategorizationRuleCondition> conditions = null;
            if (rule.getConditions() != null && !rule.getConditions().isEmpty()) {
                conditions = rule.getConditions().stream()
                        .map(c -> CategorizationRuleCondition.builder()
                                .field(c.getField())
                                .patternType(c.getPatternType())
                                .value(c.getValue())
                                .build())
                        .toList();
            }

            CategorizationRule entity = CategorizationRule.builder()
                    .userId(userId)
                    .name(rule.getName())
                    .pattern(rule.getPattern() != null ? rule.getPattern() : "")
                    .patternType(rule.getPatternType())
                    .matchField(rule.getMatchField())
                    .conditionOperator(rule.getConditionOperator() != null ? rule.getConditionOperator() : RuleConditionOperator.AND)
                    .conditions(conditions)
                    .categoryId(newCategoryId)
                    .priority(rule.getPriority())
                    .active(rule.isActive())
                    .system(false)
                    .build();
            categorizationRuleRepository.save(entity);
            count++;
        }
        return count;
    }

    private int importBudgets(Long userId, List<SnapshotBudgetMonth> budgets, Map<Long, Long> categoryIdMap) {
        if (budgets == null || budgets.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (SnapshotBudgetMonth month : budgets) {
            if (month.getTargets() == null || month.getTargets().isEmpty()) {
                continue;
            }

            List<BudgetTargetRepository.UpsertBudgetTarget> targets = new ArrayList<>();
            for (var target : month.getTargets()) {
                Long newCategoryId = categoryIdMap.get(target.getCategoryId());
                if (newCategoryId == null) {
                    continue;
                }
                targets.add(new BudgetTargetRepository.UpsertBudgetTarget(
                        newCategoryId,
                        target.getTargetAmount() != null ? target.getTargetAmount() : BigDecimal.ZERO,
                        target.getNotes()));
            }

            if (!targets.isEmpty()) {
                budgetTargetRepository.replaceMonthTargets(userId, month.getMonth(), targets);
                count += targets.size();
            }
        }
        return count;
    }

    private int importRecurringPatterns(Long userId, List<SnapshotRecurringPattern> patterns,
                                         Map<Long, Long> categoryIdMap) {
        if (patterns == null || patterns.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (SnapshotRecurringPattern pat : patterns) {
            Long newCategoryId = pat.getCategory() != null ? categoryIdMap.get(pat.getCategory().getId()) : null;

            RecurringPattern entity = RecurringPattern.builder()
                    .userId(userId)
                    .name(pat.getName())
                    .merchantPattern(pat.getMerchantPattern())
                    .expectedAmount(pat.getExpectedAmount() != null ? pat.getExpectedAmount() : BigDecimal.ZERO)
                    .amountVariance(BigDecimal.ZERO)
                    .frequency(pat.getFrequency())
                    .dayOfWeek(null)
                    .dayOfMonth(pat.getDayOfMonth())
                    .nextExpectedDate(parseLocalDateOrNull(pat.getNextExpectedDate()))
                    .categoryId(newCategoryId)
                    .bill(pat.isBill())
                    .active(pat.isActive())
                    .lastOccurrenceAt(parseInstantOrNull(pat.getLastOccurrenceAt()))
                    .build();
            recurringPatternRepository.save(entity);
            count++;
        }
        return count;
    }

    private Long resolveCategoryId(SnapshotCategory category, Map<Long, Long> categoryIdMap, Long uncategorizedCategoryId) {
        if (category == null) {
            return uncategorizedCategoryId;
        }
        Long mapped = categoryIdMap.get(category.getId());
        return mapped != null ? mapped : uncategorizedCategoryId;
    }

    private CategoryType safeCategoryType(CategoryType type) {
        if (type == null || type == CategoryType.UNCATEGORIZED) {
            return CategoryType.EXPENSE;
        }
        return type;
    }

    private boolean isUncategorizedByName(String name) {
        return name != null && name.trim().equalsIgnoreCase("Uncategorized");
    }

    private Instant parseInstantOrDefault(String value, Instant defaultValue) {
        Instant parsed = parseInstantOrNull(value);
        return parsed != null ? parsed : defaultValue;
    }

    private Instant parseInstantOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException e) {
            // Try ISO date-only format (YYYY-MM-DD)
            try {
                return LocalDate.parse(value, DateTimeFormatter.ISO_DATE)
                        .atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
            } catch (DateTimeParseException e2) {
                log.warn("Could not parse date '{}', returning null", value);
                return null;
            }
        }
    }

    private LocalDate parseLocalDateOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value, DateTimeFormatter.ISO_DATE);
        } catch (DateTimeParseException e) {
            // May be an ISO instant string, extract the date portion
            try {
                return Instant.parse(value).atZone(java.time.ZoneOffset.UTC).toLocalDate();
            } catch (DateTimeParseException e2) {
                log.warn("Could not parse local date '{}', returning null", value);
                return null;
            }
        }
    }
}
