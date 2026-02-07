package com.peter.budget.repository;

import com.peter.budget.model.entity.CategoryOverride;
import com.peter.budget.model.enums.CategoryType;
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
public class CategoryOverrideRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private static final RowMapper<CategoryOverride> ROW_MAPPER = (rs, rowNum) -> CategoryOverride.builder()
            .id(rs.getLong("id"))
            .userId(rs.getLong("user_id"))
            .categoryId(rs.getLong("category_id"))
            .parentIdOverride(rs.getObject("parent_id_override", Long.class))
            .nameOverride(rs.getString("name_override"))
            .iconOverride(rs.getString("icon_override"))
            .colorOverride(rs.getString("color_override"))
            .categoryTypeOverride(CategoryType.valueOf(rs.getString("category_type_override")))
            .hidden(rs.getBoolean("is_hidden"))
            .createdAt(rs.getTimestamp("created_at").toInstant())
            .updatedAt(rs.getTimestamp("updated_at").toInstant())
            .build();

    public List<CategoryOverride> findByUserId(Long userId) {
        String sql = "SELECT * FROM category_overrides WHERE user_id = :userId";
        var params = new MapSqlParameterSource("userId", userId);
        return jdbcTemplate.query(sql, params, ROW_MAPPER);
    }

    public Optional<CategoryOverride> findByUserIdAndCategoryId(Long userId, Long categoryId) {
        String sql = "SELECT * FROM category_overrides WHERE user_id = :userId AND category_id = :categoryId";
        var params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("categoryId", categoryId);
        var results = jdbcTemplate.query(sql, params, ROW_MAPPER);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public CategoryOverride save(CategoryOverride categoryOverride) {
        if (categoryOverride.getId() == null) {
            return insert(categoryOverride);
        }
        return update(categoryOverride);
    }

    private CategoryOverride insert(CategoryOverride categoryOverride) {
        String sql = """
            INSERT INTO category_overrides (
                user_id, category_id, parent_id_override, name_override, icon_override,
                color_override, category_type_override, is_hidden, created_at, updated_at
            )
            VALUES (
                :userId, :categoryId, :parentIdOverride, :nameOverride, :iconOverride,
                :colorOverride, :categoryTypeOverride, :isHidden, :createdAt, :updatedAt
            )
            """;

        Instant now = Instant.now();
        KeyHolder keyHolder = new GeneratedKeyHolder();

        var params = buildParams(categoryOverride)
                .addValue("createdAt", Timestamp.from(now))
                .addValue("updatedAt", Timestamp.from(now));

        jdbcTemplate.update(sql, params, keyHolder, new String[]{"id"});

        categoryOverride.setId(keyHolder.getKey().longValue());
        categoryOverride.setCreatedAt(now);
        categoryOverride.setUpdatedAt(now);
        return categoryOverride;
    }

    private CategoryOverride update(CategoryOverride categoryOverride) {
        String sql = """
            UPDATE category_overrides SET
                parent_id_override = :parentIdOverride,
                name_override = :nameOverride,
                icon_override = :iconOverride,
                color_override = :colorOverride,
                category_type_override = :categoryTypeOverride,
                is_hidden = :isHidden,
                updated_at = :updatedAt
            WHERE id = :id
            """;

        Instant now = Instant.now();
        var params = buildParams(categoryOverride)
                .addValue("id", categoryOverride.getId())
                .addValue("updatedAt", Timestamp.from(now));

        jdbcTemplate.update(sql, params);
        categoryOverride.setUpdatedAt(now);
        return categoryOverride;
    }

    private MapSqlParameterSource buildParams(CategoryOverride categoryOverride) {
        return new MapSqlParameterSource()
                .addValue("userId", categoryOverride.getUserId())
                .addValue("categoryId", categoryOverride.getCategoryId())
                .addValue("parentIdOverride", categoryOverride.getParentIdOverride())
                .addValue("nameOverride", categoryOverride.getNameOverride())
                .addValue("iconOverride", categoryOverride.getIconOverride())
                .addValue("colorOverride", categoryOverride.getColorOverride())
                .addValue("categoryTypeOverride", categoryOverride.getCategoryTypeOverride().name())
                .addValue("isHidden", categoryOverride.isHidden());
    }
}
