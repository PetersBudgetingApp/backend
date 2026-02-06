package com.peter.budget.repository;

import com.peter.budget.model.entity.SimpleFinConnection;
import com.peter.budget.model.enums.SyncStatus;
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
public class SimpleFinConnectionRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private static final RowMapper<SimpleFinConnection> ROW_MAPPER = (rs, rowNum) -> {
        Timestamp lastSync = rs.getTimestamp("last_sync_at");
        Timestamp requestsReset = rs.getTimestamp("requests_reset_at");
        return SimpleFinConnection.builder()
                .id(rs.getLong("id"))
                .userId(rs.getLong("user_id"))
                .accessUrlEncrypted(rs.getString("access_url_encrypted"))
                .institutionName(rs.getString("institution_name"))
                .lastSyncAt(lastSync != null ? lastSync.toInstant() : null)
                .syncStatus(SyncStatus.valueOf(rs.getString("sync_status")))
                .errorMessage(rs.getString("error_message"))
                .requestsToday(rs.getInt("requests_today"))
                .requestsResetAt(requestsReset != null ? requestsReset.toInstant() : null)
                .createdAt(rs.getTimestamp("created_at").toInstant())
                .updatedAt(rs.getTimestamp("updated_at").toInstant())
                .build();
    };

    public List<SimpleFinConnection> findByUserId(Long userId) {
        String sql = "SELECT * FROM simplefin_connections WHERE user_id = :userId ORDER BY created_at";
        var params = new MapSqlParameterSource("userId", userId);
        return jdbcTemplate.query(sql, params, ROW_MAPPER);
    }

    public Optional<SimpleFinConnection> findById(Long id) {
        String sql = "SELECT * FROM simplefin_connections WHERE id = :id";
        var params = new MapSqlParameterSource("id", id);
        var results = jdbcTemplate.query(sql, params, ROW_MAPPER);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public Optional<SimpleFinConnection> findByIdAndUserId(Long id, Long userId) {
        String sql = "SELECT * FROM simplefin_connections WHERE id = :id AND user_id = :userId";
        var params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("userId", userId);
        var results = jdbcTemplate.query(sql, params, ROW_MAPPER);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public List<SimpleFinConnection> findDueForSync() {
        String sql = """
            SELECT * FROM simplefin_connections
            WHERE sync_status != 'IN_PROGRESS'
              AND (last_sync_at IS NULL OR last_sync_at < :fourHoursAgo)
              AND (requests_today < 24 OR requests_reset_at < :now)
            """;
        Instant now = Instant.now();
        var params = new MapSqlParameterSource()
                .addValue("fourHoursAgo", Timestamp.from(now.minusSeconds(4 * 60 * 60)))
                .addValue("now", Timestamp.from(now));
        return jdbcTemplate.query(sql, params, ROW_MAPPER);
    }

    public SimpleFinConnection save(SimpleFinConnection connection) {
        if (connection.getId() == null) {
            return insert(connection);
        }
        return update(connection);
    }

    private SimpleFinConnection insert(SimpleFinConnection connection) {
        String sql = """
            INSERT INTO simplefin_connections (user_id, access_url_encrypted, institution_name,
                last_sync_at, sync_status, error_message, requests_today, requests_reset_at,
                created_at, updated_at)
            VALUES (:userId, :accessUrlEncrypted, :institutionName,
                :lastSyncAt, :syncStatus, :errorMessage, :requestsToday, :requestsResetAt,
                :createdAt, :updatedAt)
            """;

        Instant now = Instant.now();
        KeyHolder keyHolder = new GeneratedKeyHolder();

        var params = buildParams(connection)
                .addValue("createdAt", Timestamp.from(now))
                .addValue("updatedAt", Timestamp.from(now));

        jdbcTemplate.update(sql, params, keyHolder, new String[]{"id"});

        connection.setId(keyHolder.getKey().longValue());
        connection.setCreatedAt(now);
        connection.setUpdatedAt(now);
        return connection;
    }

    private SimpleFinConnection update(SimpleFinConnection connection) {
        String sql = """
            UPDATE simplefin_connections SET
                access_url_encrypted = :accessUrlEncrypted, institution_name = :institutionName,
                last_sync_at = :lastSyncAt, sync_status = :syncStatus, error_message = :errorMessage,
                requests_today = :requestsToday, requests_reset_at = :requestsResetAt,
                updated_at = :updatedAt
            WHERE id = :id
            """;

        Instant now = Instant.now();
        var params = buildParams(connection)
                .addValue("id", connection.getId())
                .addValue("updatedAt", Timestamp.from(now));

        jdbcTemplate.update(sql, params);
        connection.setUpdatedAt(now);
        return connection;
    }

    private MapSqlParameterSource buildParams(SimpleFinConnection c) {
        return new MapSqlParameterSource()
                .addValue("userId", c.getUserId())
                .addValue("accessUrlEncrypted", c.getAccessUrlEncrypted())
                .addValue("institutionName", c.getInstitutionName())
                .addValue("lastSyncAt", c.getLastSyncAt() != null ?
                        Timestamp.from(c.getLastSyncAt()) : null)
                .addValue("syncStatus", c.getSyncStatus().name())
                .addValue("errorMessage", c.getErrorMessage())
                .addValue("requestsToday", c.getRequestsToday())
                .addValue("requestsResetAt", c.getRequestsResetAt() != null ?
                        Timestamp.from(c.getRequestsResetAt()) : null);
    }

    public void incrementRequestCount(Long id) {
        Instant now = Instant.now();
        Instant tomorrow = now.plusSeconds(24 * 60 * 60);

        String sql = """
            UPDATE simplefin_connections SET
                requests_today = CASE
                    WHEN requests_reset_at IS NULL OR requests_reset_at < :now
                    THEN 1
                    ELSE requests_today + 1
                END,
                requests_reset_at = CASE
                    WHEN requests_reset_at IS NULL OR requests_reset_at < :now
                    THEN :tomorrow
                    ELSE requests_reset_at
                END,
                updated_at = :now
            WHERE id = :id
            """;

        var params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("now", Timestamp.from(now))
                .addValue("tomorrow", Timestamp.from(tomorrow));
        jdbcTemplate.update(sql, params);
    }

    public void deleteById(Long id) {
        String sql = "DELETE FROM simplefin_connections WHERE id = :id";
        var params = new MapSqlParameterSource("id", id);
        jdbcTemplate.update(sql, params);
    }
}
