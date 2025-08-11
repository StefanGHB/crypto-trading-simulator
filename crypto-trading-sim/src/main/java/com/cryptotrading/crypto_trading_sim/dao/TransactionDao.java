package com.cryptotrading.crypto_trading_sim.dao;

import com.cryptotrading.crypto_trading_sim.model.Transaction;
import com.cryptotrading.crypto_trading_sim.model.enums.TransactionStatus;
import com.cryptotrading.crypto_trading_sim.model.enums.TransactionType;
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
 * Data Access Object for Transaction entity operations
 */
@Repository
public class TransactionDao {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ===============================================
    // SQL QUERIES
    // ===============================================

    private static final String SELECT_ALL = """
            SELECT id, user_id, crypto_id, transaction_type, quantity, price_per_unit,
                   total_amount, fees, realized_profit_loss, balance_before, balance_after,
                   transaction_status, created_at, completed_at
            FROM transactions
            """;

    private static final String SELECT_BY_ID = SELECT_ALL + " WHERE id = ?";

    private static final String SELECT_BY_USER_ID = SELECT_ALL + " WHERE user_id = ? ORDER BY created_at DESC";

    private static final String SELECT_BY_USER_AND_CRYPTO = SELECT_ALL +
            " WHERE user_id = ? AND crypto_id = ? ORDER BY created_at DESC";

    private static final String SELECT_BY_TYPE = SELECT_ALL +
            " WHERE user_id = ? AND transaction_type = ? ORDER BY created_at DESC";

    private static final String SELECT_WITH_CRYPTO_DETAILS = """
            SELECT t.id, t.user_id, t.crypto_id, t.transaction_type, t.quantity, t.price_per_unit,
                   t.total_amount, t.fees, t.realized_profit_loss, t.balance_before, t.balance_after,
                   t.transaction_status, t.created_at, t.completed_at,
                   c.symbol, c.name
            FROM transactions t
            JOIN cryptocurrencies c ON t.crypto_id = c.id
            WHERE t.user_id = ?
            ORDER BY t.created_at DESC
            """;

    private static final String INSERT_TRANSACTION = """
            INSERT INTO transactions (user_id, crypto_id, transaction_type, quantity, price_per_unit,
                                    total_amount, fees, realized_profit_loss, balance_before, 
                                    balance_after, transaction_status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String UPDATE_TRANSACTION = """
            UPDATE transactions 
            SET transaction_status = ?, realized_profit_loss = ?, completed_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """;

    private static final String UPDATE_STATUS = """
            UPDATE transactions 
            SET transaction_status = ?, completed_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """;

    private static final String DELETE_TRANSACTION = "DELETE FROM transactions WHERE id = ?";

    private static final String DELETE_USER_TRANSACTIONS = "DELETE FROM transactions WHERE user_id = ?";

    // ===============================================
    // ROW MAPPERS
    // ===============================================

    private static final RowMapper<Transaction> TRANSACTION_ROW_MAPPER = new RowMapper<Transaction>() {
        @Override
        public Transaction mapRow(ResultSet rs, int rowNum) throws SQLException {
            Transaction transaction = new Transaction();
            transaction.setId(rs.getLong("id"));
            transaction.setUserId(rs.getLong("user_id"));
            transaction.setCryptoId(rs.getLong("crypto_id"));
            transaction.setTransactionType(TransactionType.valueOf(rs.getString("transaction_type")));
            transaction.setQuantity(rs.getBigDecimal("quantity"));
            transaction.setPricePerUnit(rs.getBigDecimal("price_per_unit"));
            transaction.setTotalAmount(rs.getBigDecimal("total_amount"));
            transaction.setFees(rs.getBigDecimal("fees"));
            transaction.setRealizedProfitLoss(rs.getBigDecimal("realized_profit_loss"));
            transaction.setBalanceBefore(rs.getBigDecimal("balance_before"));
            transaction.setBalanceAfter(rs.getBigDecimal("balance_after"));
            transaction.setTransactionStatus(TransactionStatus.valueOf(rs.getString("transaction_status")));

            // Handle timestamps
            if (rs.getTimestamp("created_at") != null) {
                transaction.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            }
            if (rs.getTimestamp("completed_at") != null) {
                transaction.setCompletedAt(rs.getTimestamp("completed_at").toLocalDateTime());
            }

            return transaction;
        }
    };

    // Inner class for transactions with crypto details
    public static class TransactionWithDetails extends Transaction {
        private String symbol;
        private String cryptoName;

        // Getters and setters
        public String getSymbol() { return symbol; }
        public void setSymbol(String symbol) { this.symbol = symbol; }

        public String getCryptoName() { return cryptoName; }
        public void setCryptoName(String cryptoName) { this.cryptoName = cryptoName; }
    }

    private static final RowMapper<TransactionWithDetails> TRANSACTION_WITH_DETAILS_MAPPER = new RowMapper<TransactionWithDetails>() {
        @Override
        public TransactionWithDetails mapRow(ResultSet rs, int rowNum) throws SQLException {
            TransactionWithDetails transaction = new TransactionWithDetails();
            transaction.setId(rs.getLong("id"));
            transaction.setUserId(rs.getLong("user_id"));
            transaction.setCryptoId(rs.getLong("crypto_id"));
            transaction.setTransactionType(TransactionType.valueOf(rs.getString("transaction_type")));
            transaction.setQuantity(rs.getBigDecimal("quantity"));
            transaction.setPricePerUnit(rs.getBigDecimal("price_per_unit"));
            transaction.setTotalAmount(rs.getBigDecimal("total_amount"));
            transaction.setFees(rs.getBigDecimal("fees"));
            transaction.setRealizedProfitLoss(rs.getBigDecimal("realized_profit_loss"));
            transaction.setBalanceBefore(rs.getBigDecimal("balance_before"));
            transaction.setBalanceAfter(rs.getBigDecimal("balance_after"));
            transaction.setTransactionStatus(TransactionStatus.valueOf(rs.getString("transaction_status")));
            transaction.setSymbol(rs.getString("symbol"));
            transaction.setCryptoName(rs.getString("name"));

            // Handle timestamps
            if (rs.getTimestamp("created_at") != null) {
                transaction.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            }
            if (rs.getTimestamp("completed_at") != null) {
                transaction.setCompletedAt(rs.getTimestamp("completed_at").toLocalDateTime());
            }

            return transaction;
        }
    };

    // ===============================================
    // CRUD OPERATIONS
    // ===============================================

    /**
     * Create a new transaction
     */
    public Transaction save(Transaction transaction) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(INSERT_TRANSACTION, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, transaction.getUserId());
            ps.setLong(2, transaction.getCryptoId());
            ps.setString(3, transaction.getTransactionType().name());
            ps.setBigDecimal(4, transaction.getQuantity());
            ps.setBigDecimal(5, transaction.getPricePerUnit());
            ps.setBigDecimal(6, transaction.getTotalAmount());
            ps.setBigDecimal(7, transaction.getFees() != null ? transaction.getFees() : BigDecimal.ZERO);
            ps.setBigDecimal(8, transaction.getRealizedProfitLoss());
            ps.setBigDecimal(9, transaction.getBalanceBefore());
            ps.setBigDecimal(10, transaction.getBalanceAfter());
            ps.setString(11, transaction.getTransactionStatus().name());
            return ps;
        }, keyHolder);

        transaction.setId(keyHolder.getKey().longValue());
        return transaction;
    }

    /**
     * Find transaction by ID
     */
    public Optional<Transaction> findById(Long id) {
        try {
            Transaction transaction = jdbcTemplate.queryForObject(SELECT_BY_ID, TRANSACTION_ROW_MAPPER, id);
            return Optional.ofNullable(transaction);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * Find all transactions for a user
     */
    public List<Transaction> findByUserId(Long userId) {
        return jdbcTemplate.query(SELECT_BY_USER_ID, TRANSACTION_ROW_MAPPER, userId);
    }

    /**
     * Find transactions for a user and specific cryptocurrency
     */
    public List<Transaction> findByUserAndCrypto(Long userId, Long cryptoId) {
        return jdbcTemplate.query(SELECT_BY_USER_AND_CRYPTO, TRANSACTION_ROW_MAPPER, userId, cryptoId);
    }

    /**
     * Find transactions by type for a user
     */
    public List<Transaction> findByUserAndType(Long userId, TransactionType transactionType) {
        return jdbcTemplate.query(SELECT_BY_TYPE, TRANSACTION_ROW_MAPPER, userId, transactionType.name());
    }

    /**
     * Find transactions with crypto details for a user
     */
    public List<TransactionWithDetails> findTransactionsWithDetailsByUserId(Long userId) {
        return jdbcTemplate.query(SELECT_WITH_CRYPTO_DETAILS, TRANSACTION_WITH_DETAILS_MAPPER, userId);
    }

    /**
     * Find recent transactions for a user (last N transactions)
     */
    public List<Transaction> findRecentTransactionsByUserId(Long userId, int limit) {
        String sql = SELECT_BY_USER_ID + " LIMIT ?";
        return jdbcTemplate.query(sql, TRANSACTION_ROW_MAPPER, userId, limit);
    }

    /**
     * Update transaction
     */
    public boolean update(Transaction transaction) {
        int rowsAffected = jdbcTemplate.update(UPDATE_TRANSACTION,
                transaction.getTransactionStatus().name(),
                transaction.getRealizedProfitLoss(),
                transaction.getId());

        return rowsAffected > 0;
    }

    /**
     * Update transaction status
     */
    public boolean updateStatus(Long transactionId, TransactionStatus status) {
        int rowsAffected = jdbcTemplate.update(UPDATE_STATUS, status.name(), transactionId);
        return rowsAffected > 0;
    }

    /**
     * Delete transaction
     */
    public boolean delete(Long transactionId) {
        int rowsAffected = jdbcTemplate.update(DELETE_TRANSACTION, transactionId);
        return rowsAffected > 0;
    }

    /**
     * Delete all transactions for a user (for account reset)
     */
    public boolean deleteAllUserTransactions(Long userId) {
        int rowsAffected = jdbcTemplate.update(DELETE_USER_TRANSACTIONS, userId);
        return rowsAffected > 0;
    }

    // ===============================================
    // BUSINESS LOGIC QUERIES
    // ===============================================

    /**
     * Get total transaction count for user
     */
    public long getUserTransactionCount(Long userId) {
        String sql = "SELECT COUNT(*) FROM transactions WHERE user_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userId);
        return count != null ? count.longValue() : 0L;
    }

    /**
     * Get total buy transactions value for user
     */
    public BigDecimal getTotalBuyAmount(Long userId) {
        String sql = """
            SELECT COALESCE(SUM(total_amount), 0) 
            FROM transactions 
            WHERE user_id = ? AND transaction_type = 'BUY' AND transaction_status = 'COMPLETED'
            """;
        BigDecimal result = jdbcTemplate.queryForObject(sql, BigDecimal.class, userId);
        return result != null ? result : BigDecimal.ZERO;
    }

    /**
     * Get total sell transactions value for user
     */
    public BigDecimal getTotalSellAmount(Long userId) {
        String sql = """
            SELECT COALESCE(SUM(total_amount), 0) 
            FROM transactions 
            WHERE user_id = ? AND transaction_type = 'SELL' AND transaction_status = 'COMPLETED'
            """;
        BigDecimal result = jdbcTemplate.queryForObject(sql, BigDecimal.class, userId);
        return result != null ? result : BigDecimal.ZERO;
    }

    /**
     * Get total realized profit/loss for user
     */
    public BigDecimal getTotalRealizedProfitLoss(Long userId) {
        String sql = """
            SELECT COALESCE(SUM(realized_profit_loss), 0) 
            FROM transactions 
            WHERE user_id = ? AND transaction_type = 'SELL' AND transaction_status = 'COMPLETED'
            AND realized_profit_loss IS NOT NULL
            """;
        BigDecimal result = jdbcTemplate.queryForObject(sql, BigDecimal.class, userId);
        return result != null ? result : BigDecimal.ZERO;
    }

    /**
     * Get user's buy transactions for specific crypto (for FIFO calculation)
     */
    public List<Transaction> getUserBuyTransactionsForCrypto(Long userId, Long cryptoId) {
        String sql = SELECT_BY_USER_AND_CRYPTO +
                " AND transaction_type = 'BUY' AND transaction_status = 'COMPLETED' ORDER BY created_at ASC";
        return jdbcTemplate.query(sql, TRANSACTION_ROW_MAPPER, userId, cryptoId);
    }

    /**
     * Get transactions within date range
     */
    public List<Transaction> findTransactionsByDateRange(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        String sql = SELECT_BY_USER_ID + " AND created_at BETWEEN ? AND ?";
        return jdbcTemplate.query(sql, TRANSACTION_ROW_MAPPER, userId, startDate, endDate);
    }

    /**
     * Get transaction statistics for user
     */
    public TransactionStatistics getTransactionStatistics(Long userId) {
        String sql = """
            SELECT 
                COUNT(*) as total_transactions,
                SUM(CASE WHEN transaction_type = 'BUY' THEN 1 ELSE 0 END) as buy_count,
                SUM(CASE WHEN transaction_type = 'SELL' THEN 1 ELSE 0 END) as sell_count,
                COALESCE(SUM(CASE WHEN transaction_type = 'BUY' THEN total_amount ELSE 0 END), 0) as total_bought,
                COALESCE(SUM(CASE WHEN transaction_type = 'SELL' THEN total_amount ELSE 0 END), 0) as total_sold,
                COALESCE(SUM(realized_profit_loss), 0) as total_realized_pnl
            FROM transactions 
            WHERE user_id = ? AND transaction_status = 'COMPLETED'
            """;

        return jdbcTemplate.queryForObject(sql, new RowMapper<TransactionStatistics>() {
            @Override
            public TransactionStatistics mapRow(ResultSet rs, int rowNum) throws SQLException {
                return new TransactionStatistics(
                        rs.getLong("total_transactions"),
                        rs.getLong("buy_count"),
                        rs.getLong("sell_count"),
                        rs.getBigDecimal("total_bought"),
                        rs.getBigDecimal("total_sold"),
                        rs.getBigDecimal("total_realized_pnl")
                );
            }
        }, userId);
    }

    // Inner class for transaction statistics
    public static class TransactionStatistics {
        private final long totalTransactions;
        private final long buyCount;
        private final long sellCount;
        private final BigDecimal totalBought;
        private final BigDecimal totalSold;
        private final BigDecimal totalRealizedPnl;

        public TransactionStatistics(long totalTransactions, long buyCount, long sellCount,
                                     BigDecimal totalBought, BigDecimal totalSold, BigDecimal totalRealizedPnl) {
            this.totalTransactions = totalTransactions;
            this.buyCount = buyCount;
            this.sellCount = sellCount;
            this.totalBought = totalBought;
            this.totalSold = totalSold;
            this.totalRealizedPnl = totalRealizedPnl;
        }

        // Getters
        public long getTotalTransactions() { return totalTransactions; }
        public long getBuyCount() { return buyCount; }
        public long getSellCount() { return sellCount; }
        public BigDecimal getTotalBought() { return totalBought; }
        public BigDecimal getTotalSold() { return totalSold; }
        public BigDecimal getTotalRealizedPnl() { return totalRealizedPnl; }
    }
}