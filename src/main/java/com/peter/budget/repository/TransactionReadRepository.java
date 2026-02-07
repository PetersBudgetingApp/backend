package com.peter.budget.repository;

import com.peter.budget.model.entity.Transaction;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class TransactionReadRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public List<Transaction> findByAccountId(Long accountId) {
        String sql = "SELECT * FROM transactions WHERE account_id = :accountId ORDER BY posted_at DESC";
        var params = new MapSqlParameterSource("accountId", accountId);
        return jdbcTemplate.query(sql, params, TransactionRowMappers.TRANSACTION_ROW_MAPPER);
    }

    public List<Transaction> findByUserId(Long userId) {
        String sql = """
            SELECT t.* FROM transactions t
            JOIN accounts a ON t.account_id = a.id
            WHERE a.user_id = :userId
            ORDER BY t.posted_at DESC
            """;
        var params = new MapSqlParameterSource("userId", userId);
        return jdbcTemplate.query(sql, params, TransactionRowMappers.TRANSACTION_ROW_MAPPER);
    }

    public List<Transaction> findByUserIdWithFilters(Long userId, boolean includeTransfers,
                                                      LocalDate startDate, LocalDate endDate,
                                                      Long categoryId, boolean uncategorized,
                                                      Long accountId,
                                                      int limit, int offset) {
        StringBuilder sql = new StringBuilder("""
            SELECT t.* FROM transactions t
            JOIN accounts a ON t.account_id = a.id
            WHERE a.user_id = :userId
            """);

        var params = new MapSqlParameterSource("userId", userId);

        if (!includeTransfers) {
            sql.append(" AND t.is_internal_transfer = false");
        }

        if (startDate != null) {
            sql.append(" AND t.posted_at >= :startDate");
            params.addValue("startDate", Timestamp.from(startDate.atStartOfDay().toInstant(ZoneOffset.UTC)));
        }

        if (endDate != null) {
            sql.append(" AND t.posted_at < :endDate");
            params.addValue("endDate", Timestamp.from(endDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)));
        }

        if (uncategorized) {
            sql.append(" AND t.category_id IS NULL");
        } else if (categoryId != null) {
            sql.append(" AND t.category_id = :categoryId");
            params.addValue("categoryId", categoryId);
        }

        if (accountId != null) {
            sql.append(" AND t.account_id = :accountId");
            params.addValue("accountId", accountId);
        }

        sql.append(" ORDER BY t.posted_at DESC LIMIT :limit OFFSET :offset");
        params.addValue("limit", limit);
        params.addValue("offset", offset);

        return jdbcTemplate.query(sql.toString(), params, TransactionRowMappers.TRANSACTION_ROW_MAPPER);
    }

    public List<Transaction> findTransfersByUserId(Long userId) {
        String sql = """
            SELECT t.* FROM transactions t
            JOIN accounts a ON t.account_id = a.id
            WHERE a.user_id = :userId AND t.is_internal_transfer = true
            ORDER BY t.posted_at DESC
            """;
        var params = new MapSqlParameterSource("userId", userId);
        return jdbcTemplate.query(sql, params, TransactionRowMappers.TRANSACTION_ROW_MAPPER);
    }

    public List<Transaction> findByUserIdAndCategorizationRuleId(Long userId, Long ruleId, int limit, int offset) {
        String sql = """
            SELECT t.* FROM transactions t
            JOIN accounts a ON t.account_id = a.id
            WHERE a.user_id = :userId
              AND t.categorized_by_rule_id = :ruleId
            ORDER BY t.posted_at DESC
            LIMIT :limit OFFSET :offset
            """;
        var params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("ruleId", ruleId)
                .addValue("limit", limit)
                .addValue("offset", offset);
        return jdbcTemplate.query(sql, params, TransactionRowMappers.TRANSACTION_ROW_MAPPER);
    }

    public Optional<Transaction> findById(Long id) {
        String sql = "SELECT * FROM transactions WHERE id = :id";
        var params = new MapSqlParameterSource("id", id);
        var results = jdbcTemplate.query(sql, params, TransactionRowMappers.TRANSACTION_ROW_MAPPER);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public Optional<Transaction> findByIdAndUserId(Long id, Long userId) {
        String sql = """
            SELECT t.* FROM transactions t
            JOIN accounts a ON t.account_id = a.id
            WHERE t.id = :id AND a.user_id = :userId
            """;
        var params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("userId", userId);
        var results = jdbcTemplate.query(sql, params, TransactionRowMappers.TRANSACTION_ROW_MAPPER);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public Optional<Transaction> findByAccountIdAndExternalId(Long accountId, String externalId) {
        String sql = "SELECT * FROM transactions WHERE account_id = :accountId AND external_id = :externalId";
        var params = new MapSqlParameterSource()
                .addValue("accountId", accountId)
                .addValue("externalId", externalId);
        var results = jdbcTemplate.query(sql, params, TransactionRowMappers.TRANSACTION_ROW_MAPPER);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public List<Transaction> findPotentialTransferMatches(Long userId, Long excludeAccountId,
                                                           BigDecimal oppositeAmount,
                                                           Instant startDate, Instant endDate) {
        String sql = """
            SELECT t.* FROM transactions t
            JOIN accounts a ON t.account_id = a.id
            WHERE a.user_id = :userId
              AND t.account_id != :excludeAccountId
              AND t.amount = :oppositeAmount
              AND t.posted_at BETWEEN :startDate AND :endDate
              AND t.transfer_pair_id IS NULL
            ORDER BY t.posted_at DESC
            """;
        var params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("excludeAccountId", excludeAccountId)
                .addValue("oppositeAmount", oppositeAmount)
                .addValue("startDate", Timestamp.from(startDate))
                .addValue("endDate", Timestamp.from(endDate));
        return jdbcTemplate.query(sql, params, TransactionRowMappers.TRANSACTION_ROW_MAPPER);
    }

    public List<Transaction> findUnpairedByUserId(Long userId) {
        String sql = """
            SELECT t.* FROM transactions t
            JOIN accounts a ON t.account_id = a.id
            WHERE a.user_id = :userId
              AND t.transfer_pair_id IS NULL
              AND t.is_internal_transfer = false
            ORDER BY t.posted_at DESC
            """;
        var params = new MapSqlParameterSource("userId", userId);
        return jdbcTemplate.query(sql, params, TransactionRowMappers.TRANSACTION_ROW_MAPPER);
    }

    public LocalDate findOldestPostedDateByConnectionId(Long connectionId) {
        String sql = """
            SELECT MIN(t.posted_at) FROM transactions t
            JOIN accounts a ON t.account_id = a.id
            WHERE a.connection_id = :connectionId
            """;
        var params = new MapSqlParameterSource("connectionId", connectionId);
        Timestamp ts = jdbcTemplate.queryForObject(sql, params, Timestamp.class);
        if (ts == null) {
            return null;
        }
        return ts.toInstant().atZone(ZoneOffset.UTC).toLocalDate();
    }

    public TransactionCoverageStats getCoverageByUserId(Long userId) {
        String sql = """
            SELECT
                COUNT(t.id) AS total_count,
                MIN(t.posted_at) AS oldest_posted_at,
                MAX(t.posted_at) AS newest_posted_at
            FROM transactions t
            JOIN accounts a ON t.account_id = a.id
            WHERE a.user_id = :userId
            """;

        var params = new MapSqlParameterSource("userId", userId);

        return jdbcTemplate.queryForObject(sql, params, (rs, rowNum) -> {
            Timestamp oldest = rs.getTimestamp("oldest_posted_at");
            Timestamp newest = rs.getTimestamp("newest_posted_at");
            return new TransactionCoverageStats(
                    rs.getLong("total_count"),
                    oldest != null ? oldest.toInstant() : null,
                    newest != null ? newest.toInstant() : null
            );
        });
    }

    public record TransactionCoverageStats(long totalCount, Instant oldestPostedAt, Instant newestPostedAt) {}
}
