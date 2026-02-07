package com.peter.budget.repository;

import com.peter.budget.model.entity.User;
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
public class UserRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private static final RowMapper<User> ROW_MAPPER = (rs, rowNum) -> User.builder()
            .id(rs.getLong("id"))
            .email(rs.getString("email"))
            .passwordHash(rs.getString("password_hash"))
            .createdAt(rs.getTimestamp("created_at").toInstant())
            .updatedAt(rs.getTimestamp("updated_at").toInstant())
            .build();

    public Optional<User> findById(Long id) {
        String sql = "SELECT * FROM users WHERE id = :id";
        var params = new MapSqlParameterSource("id", id);
        var results = jdbcTemplate.query(sql, params, ROW_MAPPER);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public Optional<User> findByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = :email";
        var params = new MapSqlParameterSource("email", email);
        var results = jdbcTemplate.query(sql, params, ROW_MAPPER);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public List<Long> findAllUserIds() {
        String sql = "SELECT id FROM users ORDER BY id";
        return jdbcTemplate.queryForList(sql, new MapSqlParameterSource(), Long.class);
    }

    public boolean existsByEmail(String email) {
        String sql = "SELECT COUNT(*) FROM users WHERE email = :email";
        var params = new MapSqlParameterSource("email", email);
        Integer count = jdbcTemplate.queryForObject(sql, params, Integer.class);
        return count != null && count > 0;
    }

    public User save(User user) {
        if (user.getId() == null) {
            return insert(user);
        }
        return update(user);
    }

    private User insert(User user) {
        String sql = """
            INSERT INTO users (email, password_hash, created_at, updated_at)
            VALUES (:email, :passwordHash, :createdAt, :updatedAt)
            """;

        Instant now = Instant.now();
        KeyHolder keyHolder = new GeneratedKeyHolder();

        var params = new MapSqlParameterSource()
                .addValue("email", user.getEmail())
                .addValue("passwordHash", user.getPasswordHash())
                .addValue("createdAt", Timestamp.from(now))
                .addValue("updatedAt", Timestamp.from(now));

        jdbcTemplate.update(sql, params, keyHolder, new String[]{"id"});

        user.setId(keyHolder.getKey().longValue());
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        return user;
    }

    private User update(User user) {
        String sql = """
            UPDATE users SET email = :email, password_hash = :passwordHash, updated_at = :updatedAt
            WHERE id = :id
            """;

        Instant now = Instant.now();
        var params = new MapSqlParameterSource()
                .addValue("id", user.getId())
                .addValue("email", user.getEmail())
                .addValue("passwordHash", user.getPasswordHash())
                .addValue("updatedAt", Timestamp.from(now));

        jdbcTemplate.update(sql, params);
        user.setUpdatedAt(now);
        return user;
    }
}
