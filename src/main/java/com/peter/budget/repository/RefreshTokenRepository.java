package com.peter.budget.repository;

import com.peter.budget.model.entity.RefreshToken;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private static final RowMapper<RefreshToken> ROW_MAPPER = (rs, rowNum) -> RefreshToken.builder()
            .id(rs.getLong("id"))
            .userId(rs.getLong("user_id"))
            .tokenHash(rs.getString("token_hash"))
            .expiresAt(rs.getTimestamp("expires_at").toInstant())
            .revoked(rs.getBoolean("revoked"))
            .createdAt(rs.getTimestamp("created_at").toInstant())
            .build();

    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        String sql = "SELECT * FROM refresh_tokens WHERE token_hash = :tokenHash AND revoked = false";
        var params = new MapSqlParameterSource("tokenHash", tokenHash);
        var results = jdbcTemplate.query(sql, params, ROW_MAPPER);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public RefreshToken save(RefreshToken token) {
        String sql = """
            INSERT INTO refresh_tokens (user_id, token_hash, expires_at, revoked, created_at)
            VALUES (:userId, :tokenHash, :expiresAt, :revoked, :createdAt)
            """;

        Instant now = Instant.now();
        KeyHolder keyHolder = new GeneratedKeyHolder();

        var params = new MapSqlParameterSource()
                .addValue("userId", token.getUserId())
                .addValue("tokenHash", token.getTokenHash())
                .addValue("expiresAt", Timestamp.from(token.getExpiresAt()))
                .addValue("revoked", token.isRevoked())
                .addValue("createdAt", Timestamp.from(now));

        jdbcTemplate.update(sql, params, keyHolder, new String[]{"id"});

        token.setId(keyHolder.getKey().longValue());
        token.setCreatedAt(now);
        return token;
    }

    public void revokeByUserId(Long userId) {
        String sql = "UPDATE refresh_tokens SET revoked = true WHERE user_id = :userId";
        var params = new MapSqlParameterSource("userId", userId);
        jdbcTemplate.update(sql, params);
    }

    public void revokeByTokenHash(String tokenHash) {
        String sql = "UPDATE refresh_tokens SET revoked = true WHERE token_hash = :tokenHash";
        var params = new MapSqlParameterSource("tokenHash", tokenHash);
        jdbcTemplate.update(sql, params);
    }

    public void deleteExpired() {
        String sql = "DELETE FROM refresh_tokens WHERE expires_at < :now";
        var params = new MapSqlParameterSource("now", Timestamp.from(Instant.now()));
        jdbcTemplate.update(sql, params);
    }
}
