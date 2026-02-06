package com.peter.budget.repository;

import com.peter.budget.model.entity.RecurringPattern;
import com.peter.budget.model.enums.Frequency;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RecurringPatternRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private static final RowMapper<RecurringPattern> ROW_MAPPER = (rs, rowNum) -> {
        Date nextExpected = rs.getDate("next_expected_date");
        Timestamp lastOccurrence = rs.getTimestamp("last_occurrence_at");
        return RecurringPattern.builder()
                .id(rs.getLong("id"))
                .userId(rs.getLong("user_id"))
                .name(rs.getString("name"))
                .merchantPattern(rs.getString("merchant_pattern"))
                .expectedAmount(rs.getBigDecimal("expected_amount"))
                .amountVariance(rs.getBigDecimal("amount_variance"))
                .frequency(Frequency.valueOf(rs.getString("frequency")))
                .dayOfWeek(rs.getObject("day_of_week", Integer.class))
                .dayOfMonth(rs.getObject("day_of_month", Integer.class))
                .nextExpectedDate(nextExpected != null ? nextExpected.toLocalDate() : null)
                .categoryId(rs.getObject("category_id", Long.class))
                .bill(rs.getBoolean("is_bill"))
                .active(rs.getBoolean("is_active"))
                .lastOccurrenceAt(lastOccurrence != null ? lastOccurrence.toInstant() : null)
                .createdAt(rs.getTimestamp("created_at").toInstant())
                .updatedAt(rs.getTimestamp("updated_at").toInstant())
                .build();
    };

    public List<RecurringPattern> findByUserId(Long userId) {
        String sql = "SELECT * FROM recurring_patterns WHERE user_id = :userId ORDER BY name";
        var params = new MapSqlParameterSource("userId", userId);
        return jdbcTemplate.query(sql, params, ROW_MAPPER);
    }

    public List<RecurringPattern> findActiveByUserId(Long userId) {
        String sql = """
            SELECT * FROM recurring_patterns
            WHERE user_id = :userId AND is_active = true
            ORDER BY name
            """;
        var params = new MapSqlParameterSource("userId", userId);
        return jdbcTemplate.query(sql, params, ROW_MAPPER);
    }

    public List<RecurringPattern> findUpcomingBills(Long userId, LocalDate startDate, LocalDate endDate) {
        String sql = """
            SELECT * FROM recurring_patterns
            WHERE user_id = :userId AND is_active = true AND is_bill = true
              AND next_expected_date BETWEEN :startDate AND :endDate
            ORDER BY next_expected_date
            """;
        var params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("startDate", Date.valueOf(startDate))
                .addValue("endDate", Date.valueOf(endDate));
        return jdbcTemplate.query(sql, params, ROW_MAPPER);
    }

    public List<RecurringPattern> findBillsForMonth(Long userId, int year, int month) {
        LocalDate startOfMonth = LocalDate.of(year, month, 1);
        LocalDate endOfMonth = startOfMonth.plusMonths(1).minusDays(1);
        return findUpcomingBills(userId, startOfMonth, endOfMonth);
    }

    public Optional<RecurringPattern> findById(Long id) {
        String sql = "SELECT * FROM recurring_patterns WHERE id = :id";
        var params = new MapSqlParameterSource("id", id);
        var results = jdbcTemplate.query(sql, params, ROW_MAPPER);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public Optional<RecurringPattern> findByIdAndUserId(Long id, Long userId) {
        String sql = "SELECT * FROM recurring_patterns WHERE id = :id AND user_id = :userId";
        var params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("userId", userId);
        var results = jdbcTemplate.query(sql, params, ROW_MAPPER);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public Optional<RecurringPattern> findByMerchantPattern(Long userId, String merchantPattern) {
        String sql = """
            SELECT * FROM recurring_patterns
            WHERE user_id = :userId AND UPPER(merchant_pattern) = UPPER(:merchantPattern)
            """;
        var params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("merchantPattern", merchantPattern);
        var results = jdbcTemplate.query(sql, params, ROW_MAPPER);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public RecurringPattern save(RecurringPattern pattern) {
        if (pattern.getId() == null) {
            return insert(pattern);
        }
        return update(pattern);
    }

    private RecurringPattern insert(RecurringPattern pattern) {
        String sql = """
            INSERT INTO recurring_patterns (user_id, name, merchant_pattern, expected_amount,
                amount_variance, frequency, day_of_week, day_of_month, next_expected_date,
                category_id, is_bill, is_active, last_occurrence_at, created_at, updated_at)
            VALUES (:userId, :name, :merchantPattern, :expectedAmount,
                :amountVariance, :frequency, :dayOfWeek, :dayOfMonth, :nextExpectedDate,
                :categoryId, :isBill, :isActive, :lastOccurrenceAt, :createdAt, :updatedAt)
            """;

        Instant now = Instant.now();
        KeyHolder keyHolder = new GeneratedKeyHolder();

        var params = buildParams(pattern)
                .addValue("createdAt", Timestamp.from(now))
                .addValue("updatedAt", Timestamp.from(now));

        jdbcTemplate.update(sql, params, keyHolder, new String[]{"id"});

        pattern.setId(keyHolder.getKey().longValue());
        pattern.setCreatedAt(now);
        pattern.setUpdatedAt(now);
        return pattern;
    }

    private RecurringPattern update(RecurringPattern pattern) {
        String sql = """
            UPDATE recurring_patterns SET
                name = :name, merchant_pattern = :merchantPattern, expected_amount = :expectedAmount,
                amount_variance = :amountVariance, frequency = :frequency, day_of_week = :dayOfWeek,
                day_of_month = :dayOfMonth, next_expected_date = :nextExpectedDate,
                category_id = :categoryId, is_bill = :isBill, is_active = :isActive,
                last_occurrence_at = :lastOccurrenceAt, updated_at = :updatedAt
            WHERE id = :id
            """;

        Instant now = Instant.now();
        var params = buildParams(pattern)
                .addValue("id", pattern.getId())
                .addValue("updatedAt", Timestamp.from(now));

        jdbcTemplate.update(sql, params);
        pattern.setUpdatedAt(now);
        return pattern;
    }

    private MapSqlParameterSource buildParams(RecurringPattern p) {
        return new MapSqlParameterSource()
                .addValue("userId", p.getUserId())
                .addValue("name", p.getName())
                .addValue("merchantPattern", p.getMerchantPattern())
                .addValue("expectedAmount", p.getExpectedAmount())
                .addValue("amountVariance", p.getAmountVariance())
                .addValue("frequency", p.getFrequency().name())
                .addValue("dayOfWeek", p.getDayOfWeek())
                .addValue("dayOfMonth", p.getDayOfMonth())
                .addValue("nextExpectedDate", p.getNextExpectedDate() != null ?
                        Date.valueOf(p.getNextExpectedDate()) : null)
                .addValue("categoryId", p.getCategoryId())
                .addValue("isBill", p.isBill())
                .addValue("isActive", p.isActive())
                .addValue("lastOccurrenceAt", p.getLastOccurrenceAt() != null ?
                        Timestamp.from(p.getLastOccurrenceAt()) : null);
    }

    public void deleteById(Long id) {
        String sql = "DELETE FROM recurring_patterns WHERE id = :id";
        var params = new MapSqlParameterSource("id", id);
        jdbcTemplate.update(sql, params);
    }
}
