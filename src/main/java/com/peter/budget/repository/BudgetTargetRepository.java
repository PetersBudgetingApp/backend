package com.peter.budget.repository;

import com.peter.budget.model.entity.BudgetTarget;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class BudgetTargetRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private static final RowMapper<BudgetTarget> ROW_MAPPER = (rs, rowNum) -> BudgetTarget.builder()
            .id(rs.getLong("id"))
            .userId(rs.getLong("user_id"))
            .monthKey(rs.getString("month_key"))
            .categoryId(rs.getLong("category_id"))
            .targetAmount(rs.getBigDecimal("target_amount"))
            .notes(rs.getString("notes"))
            .createdAt(rs.getTimestamp("created_at").toInstant())
            .updatedAt(rs.getTimestamp("updated_at").toInstant())
            .build();

    public List<BudgetTarget> findByUserIdAndMonthKey(Long userId, String monthKey) {
        String sql = """
            SELECT * FROM budget_targets
            WHERE user_id = :userId
              AND month_key = :monthKey
            ORDER BY category_id
            """;

        var params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("monthKey", monthKey);

        return jdbcTemplate.query(sql, params, java.util.Objects.requireNonNull(ROW_MAPPER));
    }

    public void replaceMonthTargets(Long userId, String monthKey, List<UpsertBudgetTarget> targets) {
        String deleteSql = """
            DELETE FROM budget_targets
            WHERE user_id = :userId
              AND month_key = :monthKey
            """;

        var deleteParams = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("monthKey", monthKey);
        jdbcTemplate.update(deleteSql, deleteParams);

        if (targets.isEmpty()) {
            return;
        }

        String insertSql = """
            INSERT INTO budget_targets (user_id, month_key, category_id, target_amount, notes, created_at, updated_at)
            VALUES (:userId, :monthKey, :categoryId, :targetAmount, :notes, :createdAt, :updatedAt)
            """;

        Instant now = Instant.now();
        Timestamp nowTimestamp = Timestamp.from(now);
        MapSqlParameterSource[] batch = targets.stream()
                .map(target -> new MapSqlParameterSource()
                        .addValue("userId", userId)
                        .addValue("monthKey", monthKey)
                        .addValue("categoryId", target.categoryId())
                        .addValue("targetAmount", target.targetAmount())
                        .addValue("notes", target.notes())
                        .addValue("createdAt", nowTimestamp)
                        .addValue("updatedAt", nowTimestamp))
                .toArray(MapSqlParameterSource[]::new);

        jdbcTemplate.batchUpdate(insertSql, java.util.Objects.requireNonNull(batch));
    }

    public void deleteByUserIdAndMonthKeyAndCategoryId(Long userId, String monthKey, Long categoryId) {
        String sql = """
            DELETE FROM budget_targets
            WHERE user_id = :userId
              AND month_key = :monthKey
              AND category_id = :categoryId
            """;

        var params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("monthKey", monthKey)
                .addValue("categoryId", categoryId);
        jdbcTemplate.update(sql, params);
    }

    public record UpsertBudgetTarget(Long categoryId, BigDecimal targetAmount, String notes) {}
}
