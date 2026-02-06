package com.peter.budget.repository;

import com.peter.budget.model.entity.Category;
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
public class CategoryRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private static final RowMapper<Category> ROW_MAPPER = (rs, rowNum) -> Category.builder()
            .id(rs.getLong("id"))
            .userId(rs.getObject("user_id", Long.class))
            .parentId(rs.getObject("parent_id", Long.class))
            .name(rs.getString("name"))
            .icon(rs.getString("icon"))
            .color(rs.getString("color"))
            .categoryType(CategoryType.valueOf(rs.getString("category_type")))
            .system(rs.getBoolean("is_system"))
            .sortOrder(rs.getInt("sort_order"))
            .createdAt(rs.getTimestamp("created_at").toInstant())
            .updatedAt(rs.getTimestamp("updated_at").toInstant())
            .build();

    public List<Category> findSystemCategories() {
        String sql = "SELECT * FROM categories WHERE user_id IS NULL ORDER BY sort_order, name";
        return jdbcTemplate.query(sql, ROW_MAPPER);
    }

    public List<Category> findByUserId(Long userId) {
        String sql = """
            SELECT * FROM categories
            WHERE user_id = :userId OR user_id IS NULL
            ORDER BY sort_order, name
            """;
        var params = new MapSqlParameterSource("userId", userId);
        return jdbcTemplate.query(sql, params, ROW_MAPPER);
    }

    public List<Category> findRootCategories(Long userId) {
        String sql = """
            SELECT * FROM categories
            WHERE (user_id = :userId OR user_id IS NULL) AND parent_id IS NULL
            ORDER BY sort_order, name
            """;
        var params = new MapSqlParameterSource("userId", userId);
        return jdbcTemplate.query(sql, params, ROW_MAPPER);
    }

    public List<Category> findByParentId(Long parentId) {
        String sql = "SELECT * FROM categories WHERE parent_id = :parentId ORDER BY sort_order, name";
        var params = new MapSqlParameterSource("parentId", parentId);
        return jdbcTemplate.query(sql, params, ROW_MAPPER);
    }

    public Optional<Category> findById(Long id) {
        String sql = "SELECT * FROM categories WHERE id = :id";
        var params = new MapSqlParameterSource("id", id);
        var results = jdbcTemplate.query(sql, params, ROW_MAPPER);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public Optional<Category> findByIdForUser(Long id, Long userId) {
        String sql = """
            SELECT * FROM categories
            WHERE id = :id AND (user_id = :userId OR user_id IS NULL)
            """;
        var params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("userId", userId);
        var results = jdbcTemplate.query(sql, params, ROW_MAPPER);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public Optional<Category> findByNameAndType(String name, CategoryType type) {
        String sql = """
            SELECT * FROM categories
            WHERE UPPER(name) = UPPER(:name) AND category_type = :type AND user_id IS NULL
            """;
        var params = new MapSqlParameterSource()
                .addValue("name", name)
                .addValue("type", type.name());
        var results = jdbcTemplate.query(sql, params, ROW_MAPPER);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public Optional<Category> findTransferCategory() {
        String sql = "SELECT * FROM categories WHERE category_type = 'TRANSFER' AND user_id IS NULL LIMIT 1";
        var results = jdbcTemplate.query(sql, ROW_MAPPER);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public Category save(Category category) {
        if (category.getId() == null) {
            return insert(category);
        }
        return update(category);
    }

    private Category insert(Category category) {
        String sql = """
            INSERT INTO categories (user_id, parent_id, name, icon, color, category_type,
                is_system, sort_order, created_at, updated_at)
            VALUES (:userId, :parentId, :name, :icon, :color, :categoryType,
                :isSystem, :sortOrder, :createdAt, :updatedAt)
            """;

        Instant now = Instant.now();
        KeyHolder keyHolder = new GeneratedKeyHolder();

        var params = new MapSqlParameterSource()
                .addValue("userId", category.getUserId())
                .addValue("parentId", category.getParentId())
                .addValue("name", category.getName())
                .addValue("icon", category.getIcon())
                .addValue("color", category.getColor())
                .addValue("categoryType", category.getCategoryType().name())
                .addValue("isSystem", category.isSystem())
                .addValue("sortOrder", category.getSortOrder())
                .addValue("createdAt", Timestamp.from(now))
                .addValue("updatedAt", Timestamp.from(now));

        jdbcTemplate.update(sql, params, keyHolder, new String[]{"id"});

        category.setId(keyHolder.getKey().longValue());
        category.setCreatedAt(now);
        category.setUpdatedAt(now);
        return category;
    }

    private Category update(Category category) {
        String sql = """
            UPDATE categories SET
                parent_id = :parentId, name = :name, icon = :icon, color = :color,
                category_type = :categoryType, sort_order = :sortOrder, updated_at = :updatedAt
            WHERE id = :id
            """;

        Instant now = Instant.now();
        var params = new MapSqlParameterSource()
                .addValue("id", category.getId())
                .addValue("parentId", category.getParentId())
                .addValue("name", category.getName())
                .addValue("icon", category.getIcon())
                .addValue("color", category.getColor())
                .addValue("categoryType", category.getCategoryType().name())
                .addValue("sortOrder", category.getSortOrder())
                .addValue("updatedAt", Timestamp.from(now));

        jdbcTemplate.update(sql, params);
        category.setUpdatedAt(now);
        return category;
    }

    public void deleteById(Long id) {
        String sql = "DELETE FROM categories WHERE id = :id AND is_system = false";
        var params = new MapSqlParameterSource("id", id);
        jdbcTemplate.update(sql, params);
    }
}
