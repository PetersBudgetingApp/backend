package com.peter.budget.repository;

import com.peter.budget.model.entity.Transaction;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;

@Repository
@RequiredArgsConstructor
public class TransactionWriteRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public Transaction save(Transaction transaction) {
        if (transaction.getId() == null) {
            return insert(transaction);
        }
        return update(transaction);
    }

    private Transaction insert(Transaction transaction) {
        String sql = """
            INSERT INTO transactions (account_id, external_id, posted_at, transacted_at, amount,
                pending, description, payee, memo, category_id, is_manually_categorized,
                transfer_pair_id, is_internal_transfer, exclude_from_totals,
                is_recurring, recurring_pattern_id, notes, created_at, updated_at)
            VALUES (:accountId, :externalId, :postedAt, :transactedAt, :amount,
                :pending, :description, :payee, :memo, :categoryId, :manuallyCategorized,
                :transferPairId, :internalTransfer, :excludeFromTotals,
                :recurring, :recurringPatternId, :notes, :createdAt, :updatedAt)
            """;

        Instant now = Instant.now();
        KeyHolder keyHolder = new GeneratedKeyHolder();

        var params = buildParams(transaction)
                .addValue("createdAt", Timestamp.from(now))
                .addValue("updatedAt", Timestamp.from(now));

        jdbcTemplate.update(sql, params, keyHolder, new String[]{"id"});

        transaction.setId(keyHolder.getKey().longValue());
        transaction.setCreatedAt(now);
        transaction.setUpdatedAt(now);
        return transaction;
    }

    private Transaction update(Transaction transaction) {
        String sql = """
            UPDATE transactions SET
                posted_at = :postedAt, transacted_at = :transactedAt, amount = :amount,
                pending = :pending, description = :description, payee = :payee, memo = :memo,
                category_id = :categoryId, is_manually_categorized = :manuallyCategorized,
                transfer_pair_id = :transferPairId, is_internal_transfer = :internalTransfer,
                exclude_from_totals = :excludeFromTotals,
                is_recurring = :recurring, recurring_pattern_id = :recurringPatternId,
                notes = :notes, updated_at = :updatedAt
            WHERE id = :id
            """;

        Instant now = Instant.now();
        var params = buildParams(transaction)
                .addValue("id", transaction.getId())
                .addValue("updatedAt", Timestamp.from(now));

        jdbcTemplate.update(sql, params);
        transaction.setUpdatedAt(now);
        return transaction;
    }

    private MapSqlParameterSource buildParams(Transaction t) {
        return new MapSqlParameterSource()
                .addValue("accountId", t.getAccountId())
                .addValue("externalId", t.getExternalId())
                .addValue("postedAt", Timestamp.from(t.getPostedAt()))
                .addValue("transactedAt", t.getTransactedAt() != null
                        ? Timestamp.from(t.getTransactedAt())
                        : null)
                .addValue("amount", t.getAmount())
                .addValue("pending", t.isPending())
                .addValue("description", t.getDescription())
                .addValue("payee", t.getPayee())
                .addValue("memo", t.getMemo())
                .addValue("categoryId", t.getCategoryId())
                .addValue("manuallyCategorized", t.isManuallyCategorized())
                .addValue("transferPairId", t.getTransferPairId())
                .addValue("internalTransfer", t.isInternalTransfer())
                .addValue("excludeFromTotals", t.isExcludeFromTotals())
                .addValue("recurring", t.isRecurring())
                .addValue("recurringPatternId", t.getRecurringPatternId())
                .addValue("notes", t.getNotes());
    }

    public void linkTransferPair(Long transactionId1, Long transactionId2) {
        String sql = """
            UPDATE transactions SET
                transfer_pair_id = CASE
                    WHEN id = :id1 THEN :id2
                    WHEN id = :id2 THEN :id1
                END,
                is_internal_transfer = true,
                exclude_from_totals = true,
                updated_at = :now
            WHERE id IN (:id1, :id2)
            """;
        var params = new MapSqlParameterSource()
                .addValue("id1", transactionId1)
                .addValue("id2", transactionId2)
                .addValue("now", Timestamp.from(Instant.now()));
        jdbcTemplate.update(sql, params);
    }

    public void unlinkTransferPair(Long transactionId) {
        String sql = """
            UPDATE transactions SET
                transfer_pair_id = NULL,
                is_internal_transfer = false,
                exclude_from_totals = false,
                updated_at = :now
            WHERE id = :id OR transfer_pair_id = :id
            """;
        var params = new MapSqlParameterSource()
                .addValue("id", transactionId)
                .addValue("now", Timestamp.from(Instant.now()));
        jdbcTemplate.update(sql, params);
    }
}
