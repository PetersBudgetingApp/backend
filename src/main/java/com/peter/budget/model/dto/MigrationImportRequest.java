package com.peter.budget.model.dto;

import com.peter.budget.model.enums.AccountNetWorthCategory;
import com.peter.budget.model.enums.AccountType;
import com.peter.budget.model.enums.CategoryType;
import com.peter.budget.model.enums.Frequency;
import com.peter.budget.model.enums.MatchField;
import com.peter.budget.model.enums.PatternType;
import com.peter.budget.model.enums.RuleConditionOperator;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MigrationImportRequest {

    private String capturedAt;
    private String sourceBackend;
    private String userEmail;
    private List<SnapshotAccount> accounts;
    private List<SnapshotCategory> categories;
    private List<SnapshotRule> rules;
    private List<SnapshotTransaction> transactions;
    private List<SnapshotTransferPair> transferPairs;
    private List<SnapshotBudgetMonth> budgets;
    private List<SnapshotRecurringPattern> recurringPatterns;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SnapshotAccount {
        private Long id;
        private String name;
        private String institutionName;
        private AccountType accountType;
        private AccountNetWorthCategory netWorthCategory;
        private String currency;
        private BigDecimal currentBalance;
        private BigDecimal availableBalance;
        private String balanceUpdatedAt;
        private boolean active;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SnapshotCategory {
        private Long id;
        private Long parentId;
        private String name;
        private String icon;
        private String color;
        private CategoryType categoryType;
        private boolean system;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SnapshotRule {
        private Long id;
        private String name;
        private String pattern;
        private PatternType patternType;
        private MatchField matchField;
        private RuleConditionOperator conditionOperator;
        private List<SnapshotRuleCondition> conditions;
        private Long categoryId;
        private int priority;
        private boolean active;
        private boolean system;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SnapshotRuleCondition {
        private MatchField field;
        private PatternType patternType;
        private String value;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SnapshotTransaction {
        private Long id;
        private Long accountId;
        private String accountName;
        private String postedAt;
        private String transactedAt;
        private BigDecimal amount;
        private boolean pending;
        private String description;
        private String payee;
        private String memo;
        private SnapshotCategory category;
        private boolean manuallyCategorized;
        private boolean internalTransfer;
        private boolean excludeFromTotals;
        private Long transferPairId;
        private String transferPairAccountName;
        private boolean recurring;
        private String notes;
        private boolean manualEntry;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SnapshotTransferPair {
        private Long fromTransactionId;
        private String fromAccountName;
        private Long toTransactionId;
        private String toAccountName;
        private BigDecimal amount;
        private String date;
        private String description;
        private boolean autoDetected;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SnapshotBudgetMonth {
        private String month;
        private String currency;
        private List<SnapshotBudgetTarget> targets;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SnapshotBudgetTarget {
        private Long categoryId;
        private String categoryName;
        private BigDecimal targetAmount;
        private String notes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SnapshotRecurringPattern {
        private Long id;
        private String name;
        private String merchantPattern;
        private BigDecimal expectedAmount;
        private Frequency frequency;
        private Integer dayOfMonth;
        private String nextExpectedDate;
        private SnapshotCategory category;
        private boolean bill;
        private boolean active;
        private String lastOccurrenceAt;
    }
}
