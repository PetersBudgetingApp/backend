package com.peter.budget.repository;

import com.peter.budget.model.entity.Account;
import com.peter.budget.model.enums.AccountType;
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
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AccountRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private static final RowMapper<Account> ROW_MAPPER = (rs, rowNum) -> {
        Timestamp balanceUpdated = rs.getTimestamp("balance_updated_at");
        return Account.builder()
                .id(rs.getLong("id"))
                .userId(rs.getLong("user_id"))
                .connectionId(rs.getObject("connection_id", Long.class))
                .externalId(rs.getString("external_id"))
                .name(rs.getString("name"))
                .institutionName(rs.getString("institution_name"))
                .accountType(AccountType.valueOf(rs.getString("account_type")))
                .currency(rs.getString("currency"))
                .currentBalance(rs.getBigDecimal("current_balance"))
                .availableBalance(rs.getBigDecimal("available_balance"))
                .balanceUpdatedAt(balanceUpdated != null ? balanceUpdated.toInstant() : null)
                .active(rs.getBoolean("is_active"))
                .createdAt(rs.getTimestamp("created_at").toInstant())
                .updatedAt(rs.getTimestamp("updated_at").toInstant())
                .build();
    };

    public List<Account> findByUserId(Long userId) {
        String sql = "SELECT * FROM accounts WHERE user_id = :userId ORDER BY name";
        var params = new MapSqlParameterSource("userId", userId);
        return jdbcTemplate.query(sql, params, ROW_MAPPER);
    }

    public List<Account> findActiveByUserId(Long userId) {
        String sql = "SELECT * FROM accounts WHERE user_id = :userId AND is_active = true ORDER BY name";
        var params = new MapSqlParameterSource("userId", userId);
        return jdbcTemplate.query(sql, params, ROW_MAPPER);
    }

    public Optional<Account> findById(Long id) {
        String sql = "SELECT * FROM accounts WHERE id = :id";
        var params = new MapSqlParameterSource("id", id);
        var results = jdbcTemplate.query(sql, params, ROW_MAPPER);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public Optional<Account> findByIdAndUserId(Long id, Long userId) {
        String sql = "SELECT * FROM accounts WHERE id = :id AND user_id = :userId";
        var params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("userId", userId);
        var results = jdbcTemplate.query(sql, params, ROW_MAPPER);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public Optional<Account> findByConnectionIdAndExternalId(Long connectionId, String externalId) {
        String sql = "SELECT * FROM accounts WHERE connection_id = :connectionId AND external_id = :externalId";
        var params = new MapSqlParameterSource()
                .addValue("connectionId", connectionId)
                .addValue("externalId", externalId);
        var results = jdbcTemplate.query(sql, params, ROW_MAPPER);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public List<Account> findByConnectionId(Long connectionId) {
        String sql = "SELECT * FROM accounts WHERE connection_id = :connectionId ORDER BY name";
        var params = new MapSqlParameterSource("connectionId", connectionId);
        return jdbcTemplate.query(sql, params, ROW_MAPPER);
    }

    public int countByConnectionId(Long connectionId) {
        String sql = "SELECT COUNT(*) FROM accounts WHERE connection_id = :connectionId";
        var params = new MapSqlParameterSource("connectionId", connectionId);
        Integer count = jdbcTemplate.queryForObject(sql, params, Integer.class);
        return count != null ? count : 0;
    }

    public Account save(Account account) {
        if (account.getId() == null) {
            return insert(account);
        }
        return update(account);
    }

    private Account insert(Account account) {
        String sql = """
            INSERT INTO accounts (user_id, connection_id, external_id, name, institution_name,
                account_type, currency, current_balance, available_balance, balance_updated_at,
                is_active, created_at, updated_at)
            VALUES (:userId, :connectionId, :externalId, :name, :institutionName,
                :accountType, :currency, :currentBalance, :availableBalance, :balanceUpdatedAt,
                :isActive, :createdAt, :updatedAt)
            """;

        Instant now = Instant.now();
        KeyHolder keyHolder = new GeneratedKeyHolder();

        var params = new MapSqlParameterSource()
                .addValue("userId", account.getUserId())
                .addValue("connectionId", account.getConnectionId())
                .addValue("externalId", account.getExternalId())
                .addValue("name", account.getName())
                .addValue("institutionName", account.getInstitutionName())
                .addValue("accountType", account.getAccountType().name())
                .addValue("currency", account.getCurrency())
                .addValue("currentBalance", account.getCurrentBalance())
                .addValue("availableBalance", account.getAvailableBalance())
                .addValue("balanceUpdatedAt", account.getBalanceUpdatedAt() != null ?
                        Timestamp.from(account.getBalanceUpdatedAt()) : null)
                .addValue("isActive", account.isActive())
                .addValue("createdAt", Timestamp.from(now))
                .addValue("updatedAt", Timestamp.from(now));

        jdbcTemplate.update(sql, params, keyHolder, new String[]{"id"});

        account.setId(keyHolder.getKey().longValue());
        account.setCreatedAt(now);
        account.setUpdatedAt(now);
        return account;
    }

    private Account update(Account account) {
        String sql = """
            UPDATE accounts SET
                name = :name, institution_name = :institutionName,
                account_type = :accountType, currency = :currency,
                current_balance = :currentBalance, available_balance = :availableBalance,
                balance_updated_at = :balanceUpdatedAt, is_active = :isActive, updated_at = :updatedAt
            WHERE id = :id
            """;

        Instant now = Instant.now();
        var params = new MapSqlParameterSource()
                .addValue("id", account.getId())
                .addValue("name", account.getName())
                .addValue("institutionName", account.getInstitutionName())
                .addValue("accountType", account.getAccountType().name())
                .addValue("currency", account.getCurrency())
                .addValue("currentBalance", account.getCurrentBalance())
                .addValue("availableBalance", account.getAvailableBalance())
                .addValue("balanceUpdatedAt", account.getBalanceUpdatedAt() != null ?
                        Timestamp.from(account.getBalanceUpdatedAt()) : null)
                .addValue("isActive", account.isActive())
                .addValue("updatedAt", Timestamp.from(now));

        jdbcTemplate.update(sql, params);
        account.setUpdatedAt(now);
        return account;
    }

    public BigDecimal sumBalancesByUserIdAndAccountTypes(Long userId, List<AccountType> types) {
        String sql = """
            SELECT COALESCE(SUM(current_balance), 0) FROM accounts
            WHERE user_id = :userId AND is_active = true AND account_type IN (:types)
            """;
        var params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("types", types.stream().map(AccountType::name).toList());
        return jdbcTemplate.queryForObject(sql, params, BigDecimal.class);
    }
}
