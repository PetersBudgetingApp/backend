package com.peter.budget.repository;

import com.peter.budget.model.entity.CategorizationRule;
import com.peter.budget.model.enums.MatchField;
import com.peter.budget.model.enums.PatternType;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CategorizationRuleRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private static final RowMapper<CategorizationRule> ROW_MAPPER = (rs, rowNum) -> CategorizationRule.builder()
            .id(rs.getLong("id"))
            .userId(rs.getObject("user_id", Long.class))
            .name(rs.getString("name"))
            .pattern(rs.getString("pattern"))
            .patternType(PatternType.valueOf(rs.getString("pattern_type")))
            .matchField(MatchField.valueOf(rs.getString("match_field")))
            .categoryId(rs.getLong("category_id"))
            .priority(rs.getInt("priority"))
            .active(rs.getBoolean("is_active"))
            .system(rs.getBoolean("is_system"))
            .createdAt(rs.getTimestamp("created_at").toInstant())
            .updatedAt(rs.getTimestamp("updated_at").toInstant())
            .build();

    public List<CategorizationRule> findActiveRulesForUser(Long userId) {
        String sql = """
            SELECT * FROM categorization_rules
            WHERE (user_id = :userId OR user_id IS NULL) AND is_active = true
            ORDER BY priority DESC, id ASC
            """;
        var params = new MapSqlParameterSource("userId", userId);
        return jdbcTemplate.query(sql, params, ROW_MAPPER);
    }

    public List<CategorizationRule> findByUserId(Long userId) {
        String sql = """
            SELECT * FROM categorization_rules
            WHERE user_id = :userId
            ORDER BY priority DESC, name
            """;
        var params = new MapSqlParameterSource("userId", userId);
        return jdbcTemplate.query(sql, params, ROW_MAPPER);
    }

    public Optional<CategorizationRule> findById(Long id) {
        String sql = "SELECT * FROM categorization_rules WHERE id = :id";
        var params = new MapSqlParameterSource("id", id);
        var results = jdbcTemplate.query(sql, params, ROW_MAPPER);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public CategorizationRule save(CategorizationRule rule) {
        if (rule.getId() == null) {
            return insert(rule);
        }
        return update(rule);
    }

    private CategorizationRule insert(CategorizationRule rule) {
        String sql = """
            INSERT INTO categorization_rules (user_id, name, pattern, pattern_type, match_field,
                category_id, priority, is_active, is_system, created_at, updated_at)
            VALUES (:userId, :name, :pattern, :patternType, :matchField,
                :categoryId, :priority, :isActive, :isSystem, :createdAt, :updatedAt)
            """;

        Instant now = Instant.now();
        KeyHolder keyHolder = new GeneratedKeyHolder();

        var params = new MapSqlParameterSource()
                .addValue("userId", rule.getUserId())
                .addValue("name", rule.getName())
                .addValue("pattern", rule.getPattern())
                .addValue("patternType", rule.getPatternType().name())
                .addValue("matchField", rule.getMatchField().name())
                .addValue("categoryId", rule.getCategoryId())
                .addValue("priority", rule.getPriority())
                .addValue("isActive", rule.isActive())
                .addValue("isSystem", rule.isSystem())
                .addValue("createdAt", Timestamp.from(now))
                .addValue("updatedAt", Timestamp.from(now));

        jdbcTemplate.update(sql, params, keyHolder, new String[]{"id"});

        rule.setId(keyHolder.getKey().longValue());
        rule.setCreatedAt(now);
        rule.setUpdatedAt(now);
        return rule;
    }

    private CategorizationRule update(CategorizationRule rule) {
        String sql = """
            UPDATE categorization_rules SET
                name = :name, pattern = :pattern, pattern_type = :patternType,
                match_field = :matchField, category_id = :categoryId, priority = :priority,
                is_active = :isActive, updated_at = :updatedAt
            WHERE id = :id
            """;

        Instant now = Instant.now();
        var params = new MapSqlParameterSource()
                .addValue("id", rule.getId())
                .addValue("name", rule.getName())
                .addValue("pattern", rule.getPattern())
                .addValue("patternType", rule.getPatternType().name())
                .addValue("matchField", rule.getMatchField().name())
                .addValue("categoryId", rule.getCategoryId())
                .addValue("priority", rule.getPriority())
                .addValue("isActive", rule.isActive())
                .addValue("updatedAt", Timestamp.from(now));

        jdbcTemplate.update(sql, params);
        rule.setUpdatedAt(now);
        return rule;
    }

    public void deleteById(Long id) {
        String sql = "DELETE FROM categorization_rules WHERE id = :id AND is_system = false";
        var params = new MapSqlParameterSource("id", id);
        jdbcTemplate.update(sql, params);
    }
}
