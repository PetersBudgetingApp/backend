package com.peter.budget.repository;

import com.peter.budget.model.entity.Transaction;
import org.springframework.jdbc.core.RowMapper;

import java.sql.Timestamp;

final class TransactionRowMappers {

    private TransactionRowMappers() {
    }

    static final RowMapper<Transaction> TRANSACTION_ROW_MAPPER = (rs, rowNum) -> {
        Timestamp transactedAt = rs.getTimestamp("transacted_at");
        return Transaction.builder()
                .id(rs.getLong("id"))
                .accountId(rs.getLong("account_id"))
                .externalId(rs.getString("external_id"))
                .postedAt(rs.getTimestamp("posted_at").toInstant())
                .transactedAt(transactedAt != null ? transactedAt.toInstant() : null)
                .amount(rs.getBigDecimal("amount"))
                .pending(rs.getBoolean("pending"))
                .description(rs.getString("description"))
                .payee(rs.getString("payee"))
                .memo(rs.getString("memo"))
                .categoryId(rs.getObject("category_id", Long.class))
                .manuallyCategorized(rs.getBoolean("is_manually_categorized"))
                .transferPairId(rs.getObject("transfer_pair_id", Long.class))
                .internalTransfer(rs.getBoolean("is_internal_transfer"))
                .excludeFromTotals(rs.getBoolean("exclude_from_totals"))
                .recurring(rs.getBoolean("is_recurring"))
                .recurringPatternId(rs.getObject("recurring_pattern_id", Long.class))
                .notes(rs.getString("notes"))
                .createdAt(rs.getTimestamp("created_at").toInstant())
                .updatedAt(rs.getTimestamp("updated_at").toInstant())
                .build();
    };
}
