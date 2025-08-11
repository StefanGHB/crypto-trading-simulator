package com.cryptotrading.crypto_trading_sim.dao;

import com.cryptotrading.crypto_trading_sim.model.UserHolding;
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
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for UserHolding entity operations
 */
@Repository
public class UserHoldingDao {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ===============================================
    // SQL QUERIES
    // ===============================================

    private static final String SELECT_ALL = """
            SELECT id, user_id, crypto_id, quantity, average_buy_price, total_invested,
                   unrealized_profit_loss, first_purchase_at, last_updated
            FROM user_holdings
            """;

    private static final String SELECT_BY_ID = SELECT_ALL + " WHERE id = ?";

    private static final String SELECT_BY_USER_ID = SELECT_ALL + " WHERE user_id = ?";

    private static final String SELECT_BY_USER_AND_CRYPTO = SELECT_ALL + " WHERE user_id = ? AND crypto_id = ?";

    private static final String SELECT_POSITIVE_HOLDINGS = SELECT_ALL + " WHERE user_id = ? AND quantity > 0";

    private static final String SELECT_WITH_CRYPTO_DETAILS = """
            SELECT uh.id, uh.user_id, uh.crypto_id, uh.quantity, uh.average_buy_price, 
                   uh.total_invested, uh.unrealized_profit_loss, uh.first_purchase_at, uh.last_updated,
                   c.symbol, c.name, c.current_price
            FROM user_holdings uh
            JOIN cryptocurrencies c ON uh.crypto_id = c.id
            WHERE uh.user_id = ? AND uh.quantity > 0 AND c.is_active = true
            ORDER BY uh.total_invested DESC
            """;

    private static final String INSERT_HOLDING = """
            INSERT INTO user_holdings (user_id, crypto_id, quantity, average_buy_price, 
                                     total_invested, unrealized_profit_loss)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

    private static final String UPDATE_HOLDING = """
            UPDATE user_holdings 
            SET quantity = ?, average_buy_price = ?, total_invested = ?, 
                unrealized_profit_loss = ?, last_updated = CURRENT_TIMESTAMP
            WHERE id = ?
            """;

    private static final String UPDATE_QUANTITY_AND_AVERAGE_PRICE = """
            UPDATE user_holdings 
            SET quantity = ?, average_buy_price = ?, total_invested = ?, last_updated = CURRENT_TIMESTAMP
            WHERE user_id = ? AND crypto_id = ?
            """;

    private static final String UPDATE_UNREALIZED_PROFIT_LOSS = """
            UPDATE user_holdings 
            SET unrealized_profit_loss = ?, last_updated = CURRENT_TIMESTAMP
            WHERE id = ?
            """;

    private static final String DELETE_HOLDING = "DELETE FROM user_holdings WHERE id = ?";

    private static final String DELETE_ZERO_HOLDINGS = "DELETE FROM user_holdings WHERE quantity = 0";

    private static final String DELETE_USER_HOLDINGS = "DELETE FROM user_holdings WHERE user_id = ?";

    // ===============================================
    // ROW MAPPERS
    // ===============================================

    private static final RowMapper<UserHolding> HOLDING_ROW_MAPPER = new RowMapper<UserHolding>() {
        @Override
        public UserHolding mapRow(ResultSet rs, int rowNum) throws SQLException {
            UserHolding holding = new UserHolding();
            holding.setId(rs.getLong("id"));
            holding.setUserId(rs.getLong("user_id"));
            holding.setCryptoId(rs.getLong("crypto_id"));
            holding.setQuantity(rs.getBigDecimal("quantity"));
            holding.setAverageBuyPrice(rs.getBigDecimal("average_buy_price"));
            holding.setTotalInvested(rs.getBigDecimal("total_invested"));
            holding.setUnrealizedProfitLoss(rs.getBigDecimal("unrealized_profit_loss"));

            // Handle timestamps
            if (rs.getTimestamp("first_purchase_at") != null) {
                holding.setFirstPurchaseAt(rs.getTimestamp("first_purchase_at").toLocalDateTime());
            }
            if (rs.getTimestamp("last_updated") != null) {
                holding.setLastUpdated(rs.getTimestamp("last_updated").toLocalDateTime());
            }

            return holding;
        }
    };

    // Inner class for holdings with crypto details
    public static class HoldingWithDetails extends UserHolding {
        private String symbol;
        private String cryptoName;
        private BigDecimal currentPrice;

        // Getters and setters
        public String getSymbol() { return symbol; }
        public void setSymbol(String symbol) { this.symbol = symbol; }

        public String getCryptoName() { return cryptoName; }
        public void setCryptoName(String cryptoName) { this.cryptoName = cryptoName; }

        public BigDecimal getCurrentPrice() { return currentPrice; }
        public void setCurrentPrice(BigDecimal currentPrice) { this.currentPrice = currentPrice; }
    }

    private static final RowMapper<HoldingWithDetails> HOLDING_WITH_DETAILS_MAPPER = new RowMapper<HoldingWithDetails>() {
        @Override
        public HoldingWithDetails mapRow(ResultSet rs, int rowNum) throws SQLException {
            HoldingWithDetails holding = new HoldingWithDetails();
            holding.setId(rs.getLong("id"));
            holding.setUserId(rs.getLong("user_id"));
            holding.setCryptoId(rs.getLong("crypto_id"));
            holding.setQuantity(rs.getBigDecimal("quantity"));
            holding.setAverageBuyPrice(rs.getBigDecimal("average_buy_price"));
            holding.setTotalInvested(rs.getBigDecimal("total_invested"));
            holding.setUnrealizedProfitLoss(rs.getBigDecimal("unrealized_profit_loss"));
            holding.setSymbol(rs.getString("symbol"));
            holding.setCryptoName(rs.getString("name"));
            holding.setCurrentPrice(rs.getBigDecimal("current_price"));

            // Handle timestamps
            if (rs.getTimestamp("first_purchase_at") != null) {
                holding.setFirstPurchaseAt(rs.getTimestamp("first_purchase_at").toLocalDateTime());
            }
            if (rs.getTimestamp("last_updated") != null) {
                holding.setLastUpdated(rs.getTimestamp("last_updated").toLocalDateTime());
            }

            return holding;
        }
    };

    // ===============================================
    // CRUD OPERATIONS
    // ===============================================

    /**
     * Create a new user holding
     */
    public UserHolding save(UserHolding holding) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(INSERT_HOLDING, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, holding.getUserId());
            ps.setLong(2, holding.getCryptoId());
            ps.setBigDecimal(3, holding.getQuantity());
            ps.setBigDecimal(4, holding.getAverageBuyPrice());
            ps.setBigDecimal(5, holding.getTotalInvested());
            ps.setBigDecimal(6, holding.getUnrealizedProfitLoss() != null ? holding.getUnrealizedProfitLoss() : BigDecimal.ZERO);
            return ps;
        }, keyHolder);

        holding.setId(keyHolder.getKey().longValue());
        return holding;
    }

    /**
     * Find holding by ID
     */
    public Optional<UserHolding> findById(Long id) {
        try {
            UserHolding holding = jdbcTemplate.queryForObject(SELECT_BY_ID, HOLDING_ROW_MAPPER, id);
            return Optional.ofNullable(holding);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * Find all holdings for a user
     */
    public List<UserHolding> findByUserId(Long userId) {
        return jdbcTemplate.query(SELECT_BY_USER_ID, HOLDING_ROW_MAPPER, userId);
    }

    /**
     * Find holdings with positive quantity for a user
     */
    public List<UserHolding> findPositiveHoldingsByUserId(Long userId) {
        return jdbcTemplate.query(SELECT_POSITIVE_HOLDINGS, HOLDING_ROW_MAPPER, userId);
    }

    /**
     * Find specific holding for user and cryptocurrency
     */
    public Optional<UserHolding> findByUserAndCrypto(Long userId, Long cryptoId) {
        try {
            UserHolding holding = jdbcTemplate.queryForObject(SELECT_BY_USER_AND_CRYPTO, HOLDING_ROW_MAPPER, userId, cryptoId);
            return Optional.ofNullable(holding);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * Find holdings with crypto details for a user
     */
    public List<HoldingWithDetails> findHoldingsWithDetailsByUserId(Long userId) {
        return jdbcTemplate.query(SELECT_WITH_CRYPTO_DETAILS, HOLDING_WITH_DETAILS_MAPPER, userId);
    }

    /**
     * Update holding
     */
    public boolean update(UserHolding holding) {
        int rowsAffected = jdbcTemplate.update(UPDATE_HOLDING,
                holding.getQuantity(),
                holding.getAverageBuyPrice(),
                holding.getTotalInvested(),
                holding.getUnrealizedProfitLoss(),
                holding.getId());

        return rowsAffected > 0;
    }

    /**
     * Update or create holding for user and crypto
     */
    public boolean upsertHolding(Long userId, Long cryptoId, BigDecimal quantity,
                                 BigDecimal averagePrice, BigDecimal totalInvested) {
        // Try to update existing holding first
        int rowsAffected = jdbcTemplate.update(UPDATE_QUANTITY_AND_AVERAGE_PRICE,
                quantity, averagePrice, totalInvested, userId, cryptoId);

        if (rowsAffected == 0) {
            // Create new holding if none exists
            UserHolding newHolding = new UserHolding(userId, cryptoId, quantity, averagePrice);
            save(newHolding);
            return true;
        }

        return rowsAffected > 0;
    }

    /**
     * Update unrealized profit/loss
     */
    public boolean updateUnrealizedProfitLoss(Long holdingId, BigDecimal profitLoss) {
        int rowsAffected = jdbcTemplate.update(UPDATE_UNREALIZED_PROFIT_LOSS, profitLoss, holdingId);
        return rowsAffected > 0;
    }

    /**
     * Delete holding
     */
    public boolean delete(Long holdingId) {
        int rowsAffected = jdbcTemplate.update(DELETE_HOLDING, holdingId);
        return rowsAffected > 0;
    }

    /**
     * Delete all holdings for a user (for account reset)
     */
    public boolean deleteAllUserHoldings(Long userId) {
        int rowsAffected = jdbcTemplate.update(DELETE_USER_HOLDINGS, userId);
        return rowsAffected > 0;
    }

    /**
     * Clean up holdings with zero quantity
     */
    public int deleteZeroQuantityHoldings() {
        return jdbcTemplate.update(DELETE_ZERO_HOLDINGS);
    }

    // ===============================================
    // BUSINESS LOGIC QUERIES
    // ===============================================

    /**
     * Check if user has holding for specific cryptocurrency
     */
    public boolean userHasCrypto(Long userId, Long cryptoId) {
        String sql = "SELECT COUNT(*) FROM user_holdings WHERE user_id = ? AND crypto_id = ? AND quantity > 0";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userId, cryptoId);
        return count != null && count > 0;
    }

    /**
     * Get user's quantity for specific cryptocurrency
     */
    public BigDecimal getUserCryptoQuantity(Long userId, Long cryptoId) {
        String sql = "SELECT quantity FROM user_holdings WHERE user_id = ? AND crypto_id = ?";
        try {
            return jdbcTemplate.queryForObject(sql, BigDecimal.class, userId, cryptoId);
        } catch (EmptyResultDataAccessException e) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * Get total portfolio value for user at current prices
     */
    public BigDecimal getTotalPortfolioValue(Long userId) {
        String sql = """
            SELECT COALESCE(SUM(uh.quantity * c.current_price), 0)
            FROM user_holdings uh
            JOIN cryptocurrencies c ON uh.crypto_id = c.id
            WHERE uh.user_id = ? AND uh.quantity > 0 AND c.is_active = true
            """;

        BigDecimal result = jdbcTemplate.queryForObject(sql, BigDecimal.class, userId);
        return result != null ? result : BigDecimal.ZERO;
    }

    /**
     * Get total invested amount for user
     */
    public BigDecimal getTotalInvestedByUser(Long userId) {
        String sql = "SELECT COALESCE(SUM(total_invested), 0) FROM user_holdings WHERE user_id = ? AND quantity > 0";
        BigDecimal result = jdbcTemplate.queryForObject(sql, BigDecimal.class, userId);
        return result != null ? result : BigDecimal.ZERO;
    }

    /**
     * Get total unrealized profit/loss for user
     */
    public BigDecimal getTotalUnrealizedProfitLoss(Long userId) {
        String sql = """
            SELECT COALESCE(SUM((uh.quantity * c.current_price) - uh.total_invested), 0)
            FROM user_holdings uh
            JOIN cryptocurrencies c ON uh.crypto_id = c.id
            WHERE uh.user_id = ? AND uh.quantity > 0 AND c.is_active = true
            """;

        BigDecimal result = jdbcTemplate.queryForObject(sql, BigDecimal.class, userId);
        return result != null ? result : BigDecimal.ZERO;
    }

    /**
     * Get number of different cryptocurrencies user holds
     */
    public long getUserHoldingCount(Long userId) {
        String sql = "SELECT COUNT(*) FROM user_holdings WHERE user_id = ? AND quantity > 0";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userId);
        return count != null ? count.longValue() : 0L;
    }

    /**
     * Update all unrealized profit/loss for user's holdings
     */
    public void updateAllUnrealizedProfitLoss(Long userId) {
        String sql = """
            UPDATE user_holdings uh
            JOIN cryptocurrencies c ON uh.crypto_id = c.id
            SET uh.unrealized_profit_loss = (uh.quantity * c.current_price) - uh.total_invested,
                uh.last_updated = CURRENT_TIMESTAMP
            WHERE uh.user_id = ? AND uh.quantity > 0 AND c.is_active = true
            """;

        jdbcTemplate.update(sql, userId);
    }
}