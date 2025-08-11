package com.cryptotrading.crypto_trading_sim.dao;

import com.cryptotrading.crypto_trading_sim.model.Cryptocurrency;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
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
 * Data Access Object for Cryptocurrency entity operations
 */
@Repository
public class CryptocurrencyDao {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ===============================================
    // SQL QUERIES
    // ===============================================

    private static final String SELECT_ALL = """
            SELECT id, symbol, name, kraken_pair_name, current_price, price_change_24h,
                   price_change_percent_24h, market_cap_rank, is_active, last_price_update,
                   created_at, updated_at
            FROM cryptocurrencies
            """;

    private static final String SELECT_BY_ID = SELECT_ALL + " WHERE id = ?";

    private static final String SELECT_BY_SYMBOL = SELECT_ALL + " WHERE symbol = ?";

    private static final String SELECT_BY_KRAKEN_PAIR = SELECT_ALL + " WHERE kraken_pair_name = ?";

    private static final String SELECT_ACTIVE = SELECT_ALL + " WHERE is_active = true";

    private static final String SELECT_TOP_BY_RANK = SELECT_ALL +
            " WHERE is_active = true ORDER BY market_cap_rank ASC LIMIT ?";

    private static final String INSERT_CRYPTO = """
            INSERT INTO cryptocurrencies (symbol, name, kraken_pair_name, current_price,
                                        price_change_24h, price_change_percent_24h, 
                                        market_cap_rank, is_active)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String UPDATE_CRYPTO = """
            UPDATE cryptocurrencies 
            SET name = ?, kraken_pair_name = ?, current_price = ?, price_change_24h = ?,
                price_change_percent_24h = ?, market_cap_rank = ?, is_active = ?,
                last_price_update = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """;

    private static final String UPDATE_PRICE = """
            UPDATE cryptocurrencies 
            SET current_price = ?, last_price_update = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """;

    private static final String UPDATE_PRICE_BY_SYMBOL = """
            UPDATE cryptocurrencies 
            SET current_price = ?, last_price_update = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
            WHERE symbol = ?
            """;

    private static final String UPDATE_PRICE_CHANGES = """
            UPDATE cryptocurrencies 
            SET current_price = ?, price_change_24h = ?, price_change_percent_24h = ?,
                last_price_update = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
            WHERE symbol = ?
            """;

    private static final String DEACTIVATE_CRYPTO = """
            UPDATE cryptocurrencies 
            SET is_active = false, updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """;

    // ===============================================
    // ROW MAPPER
    // ===============================================

    private static final RowMapper<Cryptocurrency> CRYPTO_ROW_MAPPER = new RowMapper<Cryptocurrency>() {
        @Override
        public Cryptocurrency mapRow(ResultSet rs, int rowNum) throws SQLException {
            Cryptocurrency crypto = new Cryptocurrency();
            crypto.setId(rs.getLong("id"));
            crypto.setSymbol(rs.getString("symbol"));
            crypto.setName(rs.getString("name"));
            crypto.setKrakenPairName(rs.getString("kraken_pair_name"));
            crypto.setCurrentPrice(rs.getBigDecimal("current_price"));
            crypto.setPriceChange24h(rs.getBigDecimal("price_change_24h"));
            crypto.setPriceChangePercent24h(rs.getBigDecimal("price_change_percent_24h"));
            crypto.setMarketCapRank(rs.getInt("market_cap_rank"));
            crypto.setActive(rs.getBoolean("is_active"));

            // Handle timestamps
            if (rs.getTimestamp("last_price_update") != null) {
                crypto.setLastPriceUpdate(rs.getTimestamp("last_price_update").toLocalDateTime());
            }
            if (rs.getTimestamp("created_at") != null) {
                crypto.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            }
            if (rs.getTimestamp("updated_at") != null) {
                crypto.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
            }

            return crypto;
        }
    };

    // ===============================================
    // CRUD OPERATIONS
    // ===============================================

    /**
     * Create a new cryptocurrency
     */
    public Cryptocurrency save(Cryptocurrency crypto) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(INSERT_CRYPTO, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, crypto.getSymbol());
            ps.setString(2, crypto.getName());
            ps.setString(3, crypto.getKrakenPairName());
            ps.setBigDecimal(4, crypto.getCurrentPrice() != null ? crypto.getCurrentPrice() : BigDecimal.ZERO);
            ps.setBigDecimal(5, crypto.getPriceChange24h() != null ? crypto.getPriceChange24h() : BigDecimal.ZERO);
            ps.setBigDecimal(6, crypto.getPriceChangePercent24h() != null ? crypto.getPriceChangePercent24h() : BigDecimal.ZERO);
            ps.setInt(7, crypto.getMarketCapRank() != null ? crypto.getMarketCapRank() : 999);
            ps.setBoolean(8, crypto.isActive());
            return ps;
        }, keyHolder);

        crypto.setId(keyHolder.getKey().longValue());
        return crypto;
    }

    /**
     * Find cryptocurrency by ID
     */
    public Optional<Cryptocurrency> findById(Long id) {
        try {
            Cryptocurrency crypto = jdbcTemplate.queryForObject(SELECT_BY_ID, CRYPTO_ROW_MAPPER, id);
            return Optional.ofNullable(crypto);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * Find cryptocurrency by symbol
     */
    public Optional<Cryptocurrency> findBySymbol(String symbol) {
        try {
            Cryptocurrency crypto = jdbcTemplate.queryForObject(SELECT_BY_SYMBOL, CRYPTO_ROW_MAPPER, symbol);
            return Optional.ofNullable(crypto);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * Find cryptocurrency by Kraken pair name
     */
    public Optional<Cryptocurrency> findByKrakenPair(String krakenPairName) {
        try {
            Cryptocurrency crypto = jdbcTemplate.queryForObject(SELECT_BY_KRAKEN_PAIR, CRYPTO_ROW_MAPPER, krakenPairName);
            return Optional.ofNullable(crypto);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * Find all active cryptocurrencies
     */
    public List<Cryptocurrency> findAllActive() {
        return jdbcTemplate.query(SELECT_ACTIVE, CRYPTO_ROW_MAPPER);
    }

    /**
     * Find all cryptocurrencies
     */
    public List<Cryptocurrency> findAll() {
        return jdbcTemplate.query(SELECT_ALL, CRYPTO_ROW_MAPPER);
    }

    /**
     * Find top N cryptocurrencies by market cap rank
     */
    public List<Cryptocurrency> findTopByRank(int limit) {
        return jdbcTemplate.query(SELECT_TOP_BY_RANK, CRYPTO_ROW_MAPPER, limit);
    }

    /**
     * Update cryptocurrency
     */
    public boolean update(Cryptocurrency crypto) {
        int rowsAffected = jdbcTemplate.update(UPDATE_CRYPTO,
                crypto.getName(),
                crypto.getKrakenPairName(),
                crypto.getCurrentPrice(),
                crypto.getPriceChange24h(),
                crypto.getPriceChangePercent24h(),
                crypto.getMarketCapRank(),
                crypto.isActive(),
                crypto.getId());

        return rowsAffected > 0;
    }

    /**
     * Update cryptocurrency price
     */
    public boolean updatePrice(Long cryptoId, BigDecimal newPrice) {
        int rowsAffected = jdbcTemplate.update(UPDATE_PRICE, newPrice, cryptoId);
        return rowsAffected > 0;
    }

    /**
     * Update cryptocurrency price by symbol
     */
    public boolean updatePriceBySymbol(String symbol, BigDecimal newPrice) {
        int rowsAffected = jdbcTemplate.update(UPDATE_PRICE_BY_SYMBOL, newPrice, symbol);
        return rowsAffected > 0;
    }

    /**
     * Update cryptocurrency price with 24h changes
     */
    public boolean updatePriceWithChanges(String symbol, BigDecimal newPrice,
                                          BigDecimal priceChange24h, BigDecimal priceChangePercent24h) {
        int rowsAffected = jdbcTemplate.update(UPDATE_PRICE_CHANGES,
                newPrice, priceChange24h, priceChangePercent24h, symbol);
        return rowsAffected > 0;
    }

    /**
     * Batch update prices for multiple cryptocurrencies
     */
    public void batchUpdatePrices(List<Cryptocurrency> cryptos) {
        jdbcTemplate.batchUpdate(UPDATE_PRICE_CHANGES, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                Cryptocurrency crypto = cryptos.get(i);
                ps.setBigDecimal(1, crypto.getCurrentPrice());
                ps.setBigDecimal(2, crypto.getPriceChange24h());
                ps.setBigDecimal(3, crypto.getPriceChangePercent24h());
                ps.setString(4, crypto.getSymbol());
            }

            @Override
            public int getBatchSize() {
                return cryptos.size();
            }
        });
    }

    /**
     * Deactivate cryptocurrency
     */
    public boolean deactivate(Long cryptoId) {
        int rowsAffected = jdbcTemplate.update(DEACTIVATE_CRYPTO, cryptoId);
        return rowsAffected > 0;
    }

    // ===============================================
    // BUSINESS LOGIC QUERIES
    // ===============================================

    /**
     * Check if symbol exists
     */
    public boolean existsBySymbol(String symbol) {
        String sql = "SELECT COUNT(*) FROM cryptocurrencies WHERE symbol = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, symbol);
        return count != null && count > 0;
    }

    /**
     * Check if Kraken pair exists
     */
    public boolean existsByKrakenPair(String krakenPairName) {
        String sql = "SELECT COUNT(*) FROM cryptocurrencies WHERE kraken_pair_name = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, krakenPairName);
        return count != null && count > 0;
    }

    /**
     * Get current price by symbol
     */
    public BigDecimal getCurrentPrice(String symbol) {
        String sql = "SELECT current_price FROM cryptocurrencies WHERE symbol = ? AND is_active = true";
        try {
            return jdbcTemplate.queryForObject(sql, BigDecimal.class, symbol);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    /**
     * Get all Kraken pair names for active cryptocurrencies
     */
    public List<String> getAllActiveKrakenPairs() {
        String sql = "SELECT kraken_pair_name FROM cryptocurrencies WHERE is_active = true ORDER BY market_cap_rank";
        return jdbcTemplate.queryForList(sql, String.class);
    }

    /**
     * Get cryptocurrency ID by symbol
     */
    public Long getIdBySymbol(String symbol) {
        String sql = "SELECT id FROM cryptocurrencies WHERE symbol = ? AND is_active = true";
        try {
            return jdbcTemplate.queryForObject(sql, Long.class, symbol);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    /**
     * Get total number of active cryptocurrencies
     */
    public long getActiveCryptocurrencyCount() {
        String sql = "SELECT COUNT(*) FROM cryptocurrencies WHERE is_active = true";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        return count != null ? count.longValue() : 0L;
    }

    /**
     * Get cryptocurrencies that need price updates (older than specified minutes)
     */
    public List<Cryptocurrency> findCryptosNeedingPriceUpdate(int minutesOld) {
        String sql = SELECT_ALL +
                " WHERE is_active = true AND (last_price_update IS NULL OR last_price_update < DATE_SUB(NOW(), INTERVAL ? MINUTE))";
        return jdbcTemplate.query(sql, CRYPTO_ROW_MAPPER, minutesOld);
    }
}