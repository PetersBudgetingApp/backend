package com.peter.budget.repository;

import com.peter.budget.model.entity.Transaction;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
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
public class TransactionRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private static final RowMapper<Transaction> ROW_MAPPER = (rs, rowNum) -> {
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

    public List<Transaction> findByAccountId(Long accountId) {
        String sql = "SELECT * FROM transactions WHERE account_id = :accountId ORDER BY posted_at DESC";
        var params = new MapSqlParameterSource("accountId", accountId);
        return jdbcTemplate.query(sql, params, ROW_MAPPER);
    }

    public List<Transaction> findByUserIdWithFilters(Long userId, boolean includeTransfers,
                                                      LocalDate startDate, LocalDate endDate,
                                                      Long categoryId, Long accountId,
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

        if (categoryId != null) {
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

        return jdbcTemplate.query(sql.toString(), params, ROW_MAPPER);
    }

    public List<Transaction> findTransfersByUserId(Long userId) {
        String sql = """
            SELECT t.* FROM transactions t
            JOIN accounts a ON t.account_id = a.id
            WHERE a.user_id = :userId AND t.is_internal_transfer = true
            ORDER BY t.posted_at DESC
            """;
        var params = new MapSqlParameterSource("userId", userId);
        return jdbcTemplate.query(sql, params, ROW_MAPPER);
    }

    public Optional<Transaction> findById(Long id) {
        String sql = "SELECT * FROM transactions WHERE id = :id";
        var params = new MapSqlParameterSource("id", id);
        var results = jdbcTemplate.query(sql, params, ROW_MAPPER);
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
        var results = jdbcTemplate.query(sql, params, ROW_MAPPER);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public Optional<Transaction> findByAccountIdAndExternalId(Long accountId, String externalId) {
        String sql = "SELECT * FROM transactions WHERE account_id = :accountId AND external_id = :externalId";
        var params = new MapSqlParameterSource()
                .addValue("accountId", accountId)
                .addValue("externalId", externalId);
        var results = jdbcTemplate.query(sql, params, ROW_MAPPER);
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
        return jdbcTemplate.query(sql, params, ROW_MAPPER);
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
        return jdbcTemplate.query(sql, params, ROW_MAPPER);
    }

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
                .addValue("transactedAt", t.getTransactedAt() != null ?
                        Timestamp.from(t.getTransactedAt()) : null)
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

    public BigDecimal sumByUserIdAndDateRangeAndType(Long userId, LocalDate startDate, LocalDate endDate,
                                                      boolean income, boolean excludeTransfers) {
        StringBuilder sql = new StringBuilder("""
            SELECT COALESCE(SUM(t.amount), 0) FROM transactions t
            JOIN accounts a ON t.account_id = a.id
            WHERE a.user_id = :userId
              AND t.posted_at >= :startDate AND t.posted_at < :endDate
            """);

        if (income) {
            sql.append(" AND t.amount > 0");
        } else {
            sql.append(" AND t.amount < 0");
        }

        if (excludeTransfers) {
            sql.append(" AND t.exclude_from_totals = false");
        }

        var params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("startDate", Timestamp.from(startDate.atStartOfDay().toInstant(ZoneOffset.UTC)))
                .addValue("endDate", Timestamp.from(endDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)));

        return jdbcTemplate.queryForObject(sql.toString(), params, BigDecimal.class);
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

    public List<Object[]> sumByCategory(Long userId, LocalDate startDate, LocalDate endDate) {
        String sql = """
            SELECT t.category_id, COALESCE(SUM(ABS(t.amount)), 0) as total, COUNT(*) as count
            FROM transactions t
            JOIN accounts a ON t.account_id = a.id
            WHERE a.user_id = :userId
              AND t.posted_at >= :startDate AND t.posted_at < :endDate
              AND t.amount < 0
              AND t.exclude_from_totals = false
            GROUP BY t.category_id
            ORDER BY total DESC
            """;

        var params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("startDate", Timestamp.from(startDate.atStartOfDay().toInstant(ZoneOffset.UTC)))
                .addValue("endDate", Timestamp.from(endDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)));

        return jdbcTemplate.query(sql, params, (rs, rowNum) -> new Object[]{
                rs.getObject("category_id", Long.class),
                rs.getBigDecimal("total"),
                rs.getInt("count")
        });
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
