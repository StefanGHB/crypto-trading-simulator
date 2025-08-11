package com.cryptotrading.crypto_trading_sim.dao;

import com.cryptotrading.crypto_trading_sim.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for User entity operations
 */
@Repository
public class UserDao {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ===============================================
    // SQL QUERIES
    // ===============================================

    private static final String SELECT_ALL = """
            SELECT id, username, email, initial_balance, current_balance, 
                   total_invested, total_profit_loss, created_at, updated_at, is_active
            FROM users
            """;

    private static final String SELECT_BY_ID = SELECT_ALL + " WHERE id = ?";

    private static final String SELECT_BY_USERNAME = SELECT_ALL + " WHERE username = ?";

    private static final String SELECT_BY_EMAIL = SELECT_ALL + " WHERE email = ?";

    private static final String SELECT_ACTIVE_USERS = SELECT_ALL + " WHERE is_active = true";

    private static final String INSERT_USER = """
            INSERT INTO users (username, email, initial_balance, current_balance, 
                             total_invested, total_profit_loss, is_active)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String UPDATE_USER = """
            UPDATE users 
            SET username = ?, email = ?, initial_balance = ?, current_balance = ?, 
                total_invested = ?, total_profit_loss = ?, is_active = ?, updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """;

    private static final String UPDATE_BALANCE = """
            UPDATE users 
            SET current_balance = ?, updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """;

    private static final String UPDATE_TOTAL_INVESTED = """
            UPDATE users 
            SET total_invested = ?, updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """;

    private static final String UPDATE_TOTAL_PROFIT_LOSS = """
            UPDATE users 
            SET total_profit_loss = ?, updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """;

    private static final String RESET_USER_ACCOUNT = """
            UPDATE users 
            SET current_balance = initial_balance, total_invested = 0.00, 
                total_profit_loss = 0.00, updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """;

    private static final String DELETE_USER = "DELETE FROM users WHERE id = ?";

    private static final String DEACTIVATE_USER = """
            UPDATE users 
            SET is_active = false, updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """;

    // ===============================================
    // ROW MAPPER
    // ===============================================

    private static final RowMapper<User> USER_ROW_MAPPER = new RowMapper<User>() {
        @Override
        public User mapRow(ResultSet rs, int rowNum) throws SQLException {
            User user = new User();
            user.setId(rs.getLong("id"));
            user.setUsername(rs.getString("username"));
            user.setEmail(rs.getString("email"));
            user.setInitialBalance(rs.getBigDecimal("initial_balance"));
            user.setCurrentBalance(rs.getBigDecimal("current_balance"));
            user.setTotalInvested(rs.getBigDecimal("total_invested"));
            user.setTotalProfitLoss(rs.getBigDecimal("total_profit_loss"));

            // Handle timestamps
            if (rs.getTimestamp("created_at") != null) {
                user.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            }
            if (rs.getTimestamp("updated_at") != null) {
                user.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
            }

            user.setActive(rs.getBoolean("is_active"));

            return user;
        }
    };

    // ===============================================
    // CRUD OPERATIONS
    // ===============================================

    /**
     * Create a new user
     */
    public User save(User user) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(INSERT_USER, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getEmail());
            ps.setBigDecimal(3, user.getInitialBalance());
            ps.setBigDecimal(4, user.getCurrentBalance());
            ps.setBigDecimal(5, user.getTotalInvested() != null ? user.getTotalInvested() : BigDecimal.ZERO);
            ps.setBigDecimal(6, user.getTotalProfitLoss() != null ? user.getTotalProfitLoss() : BigDecimal.ZERO);
            ps.setBoolean(7, user.isActive());
            return ps;
        }, keyHolder);

        user.setId(keyHolder.getKey().longValue());
        return user;
    }

    /**
     * Find user by ID
     */
    public Optional<User> findById(Long id) {
        try {
            User user = jdbcTemplate.queryForObject(SELECT_BY_ID, USER_ROW_MAPPER, id);
            return Optional.ofNullable(user);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * Find user by username
     */
    public Optional<User> findByUsername(String username) {
        try {
            User user = jdbcTemplate.queryForObject(SELECT_BY_USERNAME, USER_ROW_MAPPER, username);
            return Optional.ofNullable(user);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * Find user by email
     */
    public Optional<User> findByEmail(String email) {
        try {
            User user = jdbcTemplate.queryForObject(SELECT_BY_EMAIL, USER_ROW_MAPPER, email);
            return Optional.ofNullable(user);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * Find all active users
     */
    public List<User> findAllActive() {
        return jdbcTemplate.query(SELECT_ACTIVE_USERS, USER_ROW_MAPPER);
    }

    /**
     * Find all users
     */
    public List<User> findAll() {
        return jdbcTemplate.query(SELECT_ALL, USER_ROW_MAPPER);
    }

    /**
     * Update user
     */
    public boolean update(User user) {
        int rowsAffected = jdbcTemplate.update(UPDATE_USER,
                user.getUsername(),
                user.getEmail(),
                user.getInitialBalance(),
                user.getCurrentBalance(),
                user.getTotalInvested(),
                user.getTotalProfitLoss(),
                user.isActive(),
                user.getId());

        return rowsAffected > 0;
    }

    /**
     * Update user balance
     */
    public boolean updateBalance(Long userId, BigDecimal newBalance) {
        int rowsAffected = jdbcTemplate.update(UPDATE_BALANCE, newBalance, userId);
        return rowsAffected > 0;
    }

    /**
     * Update total invested amount
     */
    public boolean updateTotalInvested(Long userId, BigDecimal totalInvested) {
        int rowsAffected = jdbcTemplate.update(UPDATE_TOTAL_INVESTED, totalInvested, userId);
        return rowsAffected > 0;
    }

    /**
     * Update total profit/loss
     */
    public boolean updateTotalProfitLoss(Long userId, BigDecimal totalProfitLoss) {
        int rowsAffected = jdbcTemplate.update(UPDATE_TOTAL_PROFIT_LOSS, totalProfitLoss, userId);
        return rowsAffected > 0;
    }

    /**
     * Reset user account to initial state
     */
    public boolean resetAccount(Long userId) {
        int rowsAffected = jdbcTemplate.update(RESET_USER_ACCOUNT, userId);
        return rowsAffected > 0;
    }

    /**
     * Delete user permanently
     */
    public boolean delete(Long userId) {
        int rowsAffected = jdbcTemplate.update(DELETE_USER, userId);
        return rowsAffected > 0;
    }

    /**
     * Deactivate user (soft delete)
     */
    public boolean deactivate(Long userId) {
        int rowsAffected = jdbcTemplate.update(DEACTIVATE_USER, userId);
        return rowsAffected > 0;
    }

    // ===============================================
    // BUSINESS LOGIC QUERIES
    // ===============================================

    /**
     * Check if username exists
     */
    public boolean existsByUsername(String username) {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, username);
        return count != null && count > 0;
    }

    /**
     * Check if email exists
     */
    public boolean existsByEmail(String email) {
        String sql = "SELECT COUNT(*) FROM users WHERE email = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, email);
        return count != null && count > 0;
    }

    /**
     * Get user's current balance
     */
    public BigDecimal getCurrentBalance(Long userId) {
        String sql = "SELECT current_balance FROM users WHERE id = ?";
        return jdbcTemplate.queryForObject(sql, BigDecimal.class, userId);
    }

    /**
     * Check if user can afford amount
     */
    public boolean canAfford(Long userId, BigDecimal amount) {
        BigDecimal currentBalance = getCurrentBalance(userId);
        return currentBalance != null && currentBalance.compareTo(amount) >= 0;
    }

    /**
     * Get total number of users
     */
    public long getTotalUserCount() {
        String sql = "SELECT COUNT(*) FROM users";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        return count != null ? count.longValue() : 0L;
    }

    /**
     * Get total number of active users
     */
    public long getActiveUserCount() {
        String sql = "SELECT COUNT(*) FROM users WHERE is_active = true";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        return count != null ? count.longValue() : 0L;
    }
}