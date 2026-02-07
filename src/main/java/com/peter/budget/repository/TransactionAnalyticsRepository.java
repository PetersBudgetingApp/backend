package com.peter.budget.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class TransactionAnalyticsRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

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

    public List<CategorySpendingProjection> sumByCategory(Long userId, LocalDate startDate, LocalDate endDate) {
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

        return jdbcTemplate.query(sql, params, (rs, rowNum) -> new CategorySpendingProjection(
                rs.getObject("category_id", Long.class),
                rs.getBigDecimal("total"),
                rs.getInt("count")
        ));
    }

    public record CategorySpendingProjection(Long categoryId, BigDecimal totalAmount, int transactionCount) {}
}
