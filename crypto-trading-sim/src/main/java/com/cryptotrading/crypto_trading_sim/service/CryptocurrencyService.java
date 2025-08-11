package com.cryptotrading.crypto_trading_sim.service;

import com.cryptotrading.crypto_trading_sim.dao.CryptocurrencyDao;
import com.cryptotrading.crypto_trading_sim.model.Cryptocurrency;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service layer for Cryptocurrency-related business logic
 */
@Service
@Transactional
public class CryptocurrencyService {

    @Autowired
    private CryptocurrencyDao cryptocurrencyDao;

    @Value("${trading.max.decimal.places:8}")
    private int maxDecimalPlaces;

    @Value("${price.update.interval:1}")
    private int priceUpdateIntervalSeconds;

    // ===============================================
    // CRYPTOCURRENCY MANAGEMENT
    // ===============================================

    /**
     * Get all active cryptocurrencies ordered by market cap rank
     */
    @Transactional(readOnly = true)
    public List<Cryptocurrency> getAllActiveCryptocurrencies() {
        return cryptocurrencyDao.findAllActive();
    }

    /**
     * Get top N cryptocurrencies by market cap rank
     */
    @Transactional(readOnly = true)
    public List<Cryptocurrency> getTopCryptocurrencies(int limit) {
        validateLimit(limit);
        return cryptocurrencyDao.findTopByRank(limit);
    }

    /**
     * Get cryptocurrency by ID
     */
    @Transactional(readOnly = true)
    public Optional<Cryptocurrency> getCryptocurrencyById(Long cryptoId) {
        validateCryptoId(cryptoId);
        return cryptocurrencyDao.findById(cryptoId);
    }

    /**
     * Get cryptocurrency by symbol
     */
    @Transactional(readOnly = true)
    public Optional<Cryptocurrency> getCryptocurrencyBySymbol(String symbol) {
        validateSymbol(symbol);
        return cryptocurrencyDao.findBySymbol(symbol.toUpperCase());
    }

    /**
     * Get cryptocurrency by Kraken pair name
     */
    @Transactional(readOnly = true)
    public Optional<Cryptocurrency> getCryptocurrencyByKrakenPair(String krakenPairName) {
        validateKrakenPair(krakenPairName);
        return cryptocurrencyDao.findByKrakenPair(krakenPairName);
    }

    /**
     * Create new cryptocurrency
     */
    public Cryptocurrency createCryptocurrency(String symbol, String name, String krakenPairName, Integer marketCapRank) {
        validateCryptocurrencyInput(symbol, name, krakenPairName, marketCapRank);

        if (cryptocurrencyDao.existsBySymbol(symbol)) {
            throw new IllegalArgumentException("Cryptocurrency with symbol already exists: " + symbol);
        }

        if (cryptocurrencyDao.existsByKrakenPair(krakenPairName)) {
            throw new IllegalArgumentException("Cryptocurrency with Kraken pair already exists: " + krakenPairName);
        }

        Cryptocurrency crypto = new Cryptocurrency(symbol.toUpperCase(), name, krakenPairName, marketCapRank);
        return cryptocurrencyDao.save(crypto);
    }

    /**
     * Update cryptocurrency information
     */
    public Cryptocurrency updateCryptocurrency(Cryptocurrency crypto) {
        validateCryptocurrencyForUpdate(crypto);

        Optional<Cryptocurrency> existingCrypto = cryptocurrencyDao.findById(crypto.getId());
        if (existingCrypto.isEmpty()) {
            throw new IllegalArgumentException("Cryptocurrency not found with ID: " + crypto.getId());
        }

        // Check symbol uniqueness (if changed)
        if (!existingCrypto.get().getSymbol().equals(crypto.getSymbol())) {
            if (cryptocurrencyDao.existsBySymbol(crypto.getSymbol())) {
                throw new IllegalArgumentException("Symbol already exists: " + crypto.getSymbol());
            }
        }

        // Check Kraken pair uniqueness (if changed)
        if (!existingCrypto.get().getKrakenPairName().equals(crypto.getKrakenPairName())) {
            if (cryptocurrencyDao.existsByKrakenPair(crypto.getKrakenPairName())) {
                throw new IllegalArgumentException("Kraken pair already exists: " + crypto.getKrakenPairName());
            }
        }

        boolean updated = cryptocurrencyDao.update(crypto);
        if (!updated) {
            throw new RuntimeException("Failed to update cryptocurrency with ID: " + crypto.getId());
        }

        return cryptocurrencyDao.findById(crypto.getId()).orElseThrow();
    }

    // ===============================================
    // PRICE MANAGEMENT
    // ===============================================

    /**
     * Get current price for cryptocurrency by symbol
     */
    @Transactional(readOnly = true)
    public BigDecimal getCurrentPrice(String symbol) {
        validateSymbol(symbol);
        BigDecimal price = cryptocurrencyDao.getCurrentPrice(symbol.toUpperCase());
        if (price == null) {
            throw new IllegalArgumentException("Price not found for cryptocurrency: " + symbol);
        }
        return price;
    }

    /**
     * Update cryptocurrency price
     */
    public boolean updatePrice(String symbol, BigDecimal newPrice) {
        validateSymbol(symbol);
        validatePrice(newPrice);

        BigDecimal roundedPrice = newPrice.setScale(maxDecimalPlaces, RoundingMode.HALF_UP);
        return cryptocurrencyDao.updatePriceBySymbol(symbol.toUpperCase(), roundedPrice);
    }

    /**
     * Update cryptocurrency price with 24h changes
     */
    public boolean updatePriceWithChanges(String symbol, BigDecimal newPrice,
                                          BigDecimal priceChange24h, BigDecimal priceChangePercent24h) {
        validateSymbol(symbol);
        validatePrice(newPrice);

        BigDecimal roundedPrice = newPrice.setScale(maxDecimalPlaces, RoundingMode.HALF_UP);
        BigDecimal roundedChange = priceChange24h != null ?
                priceChange24h.setScale(4, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        BigDecimal roundedPercent = priceChangePercent24h != null ?
                priceChangePercent24h.setScale(4, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        return cryptocurrencyDao.updatePriceWithChanges(symbol.toUpperCase(), roundedPrice, roundedChange, roundedPercent);
    }

    /**
     * Batch update prices for multiple cryptocurrencies
     */
    public void batchUpdatePrices(List<Cryptocurrency> cryptosWithNewPrices) {
        if (cryptosWithNewPrices == null || cryptosWithNewPrices.isEmpty()) {
            return;
        }

        // Validate and round prices
        List<Cryptocurrency> validatedCryptos = cryptosWithNewPrices.stream()
                .peek(crypto -> {
                    validatePrice(crypto.getCurrentPrice());
                    crypto.setCurrentPrice(crypto.getCurrentPrice().setScale(maxDecimalPlaces, RoundingMode.HALF_UP));

                    if (crypto.getPriceChange24h() != null) {
                        crypto.setPriceChange24h(crypto.getPriceChange24h().setScale(4, RoundingMode.HALF_UP));
                    }

                    if (crypto.getPriceChangePercent24h() != null) {
                        crypto.setPriceChangePercent24h(crypto.getPriceChangePercent24h().setScale(4, RoundingMode.HALF_UP));
                    }
                })
                .collect(Collectors.toList());

        cryptocurrencyDao.batchUpdatePrices(validatedCryptos);
    }

    /**
     * Get all active Kraken trading pairs
     */
    @Transactional(readOnly = true)
    public List<String> getAllActiveKrakenPairs() {
        return cryptocurrencyDao.getAllActiveKrakenPairs();
    }

    /**
     * Get cryptocurrency ID by symbol
     */
    @Transactional(readOnly = true)
    public Long getCryptocurrencyIdBySymbol(String symbol) {
        validateSymbol(symbol);
        Long id = cryptocurrencyDao.getIdBySymbol(symbol.toUpperCase());
        if (id == null) {
            throw new IllegalArgumentException("Cryptocurrency not found with symbol: " + symbol);
        }
        return id;
    }

    // ===============================================
    // MARKET ANALYSIS
    // ===============================================

    /**
     * Get market summary for all active cryptocurrencies
     */
    @Transactional(readOnly = true)
    public MarketSummary getMarketSummary() {
        List<Cryptocurrency> allCryptos = getAllActiveCryptocurrencies();

        if (allCryptos.isEmpty()) {
            return new MarketSummary(0, BigDecimal.ZERO, BigDecimal.ZERO, 0, 0);
        }

        BigDecimal totalMarketValue = allCryptos.stream()
                .map(Cryptocurrency::getCurrentPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal averagePrice = totalMarketValue.divide(
                new BigDecimal(allCryptos.size()), maxDecimalPlaces, RoundingMode.HALF_UP);

        long positiveChanges = allCryptos.stream()
                .mapToLong(crypto -> crypto.isChangePositive() ? 1 : 0)
                .sum();

        long negativeChanges = allCryptos.size() - positiveChanges;

        return new MarketSummary(
                allCryptos.size(),
                totalMarketValue,
                averagePrice,
                positiveChanges,
                negativeChanges
        );
    }

    /**
     * Get cryptocurrencies with significant price changes (>5%)
     */
    @Transactional(readOnly = true)
    public List<Cryptocurrency> getCryptocurrenciesWithSignificantChanges() {
        return getAllActiveCryptocurrencies().stream()
                .filter(crypto -> crypto.getPriceChangePercent24h() != null &&
                        crypto.getPriceChangePercent24h().abs().compareTo(new BigDecimal("5")) > 0)
                .collect(Collectors.toList());
    }

    /**
     * Get top gainers (by percentage)
     */
    @Transactional(readOnly = true)
    public List<Cryptocurrency> getTopGainers(int limit) {
        validateLimit(limit);

        return getAllActiveCryptocurrencies().stream()
                .filter(crypto -> crypto.getPriceChangePercent24h() != null &&
                        crypto.getPriceChangePercent24h().compareTo(BigDecimal.ZERO) > 0)
                .sorted((c1, c2) -> c2.getPriceChangePercent24h().compareTo(c1.getPriceChangePercent24h()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Get top losers (by percentage)
     */
    @Transactional(readOnly = true)
    public List<Cryptocurrency> getTopLosers(int limit) {
        validateLimit(limit);

        return getAllActiveCryptocurrencies().stream()
                .filter(crypto -> crypto.getPriceChangePercent24h() != null &&
                        crypto.getPriceChangePercent24h().compareTo(BigDecimal.ZERO) < 0)
                .sorted((c1, c2) -> c1.getPriceChangePercent24h().compareTo(c2.getPriceChangePercent24h()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    // ===============================================
    // PRICE UPDATE UTILITIES
    // ===============================================

    /**
     * Get cryptocurrencies that need price updates
     */
    @Transactional(readOnly = true)
    public List<Cryptocurrency> getCryptocurrenciesNeedingPriceUpdate() {
        int minutesOld = priceUpdateIntervalSeconds / 60;
        if (minutesOld < 1) minutesOld = 1; // At least 1 minute

        return cryptocurrencyDao.findCryptosNeedingPriceUpdate(minutesOld);
    }

    /**
     * Check if cryptocurrency exists by symbol
     */
    @Transactional(readOnly = true)
    public boolean existsBySymbol(String symbol) {
        validateSymbol(symbol);
        return cryptocurrencyDao.existsBySymbol(symbol.toUpperCase());
    }

    /**
     * Get price map for all active cryptocurrencies
     */
    @Transactional(readOnly = true)
    public Map<String, BigDecimal> getAllCurrentPrices() {
        return getAllActiveCryptocurrencies().stream()
                .collect(Collectors.toMap(
                        Cryptocurrency::getSymbol,
                        Cryptocurrency::getCurrentPrice
                ));
    }

    /**
     * Deactivate cryptocurrency
     */
    public boolean deactivateCryptocurrency(Long cryptoId) {
        validateCryptoId(cryptoId);
        return cryptocurrencyDao.deactivate(cryptoId);
    }

    // ===============================================
    // VALIDATION METHODS
    // ===============================================

    private void validateCryptoId(Long cryptoId) {
        if (cryptoId == null || cryptoId <= 0) {
            throw new IllegalArgumentException("Invalid cryptocurrency ID: " + cryptoId);
        }
    }

    private void validateSymbol(String symbol) {
        if (symbol == null || symbol.trim().isEmpty()) {
            throw new IllegalArgumentException("Symbol cannot be null or empty");
        }

        if (symbol.length() > 10) {
            throw new IllegalArgumentException("Symbol cannot exceed 10 characters");
        }
    }

    private void validateKrakenPair(String krakenPair) {
        if (krakenPair == null || krakenPair.trim().isEmpty()) {
            throw new IllegalArgumentException("Kraken pair name cannot be null or empty");
        }

        if (krakenPair.length() > 20) {
            throw new IllegalArgumentException("Kraken pair name cannot exceed 20 characters");
        }
    }

    private void validatePrice(BigDecimal price) {
        if (price == null) {
            throw new IllegalArgumentException("Price cannot be null");
        }

        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Price must be positive: " + price);
        }

        if (price.compareTo(new BigDecimal("1000000000")) > 0) {
            throw new IllegalArgumentException("Price cannot exceed 1 billion");
        }
    }

    private void validateLimit(int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be positive: " + limit);
        }

        if (limit > 100) {
            throw new IllegalArgumentException("Limit cannot exceed 100: " + limit);
        }
    }

    private void validateCryptocurrencyInput(String symbol, String name, String krakenPairName, Integer marketCapRank) {
        validateSymbol(symbol);

        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }

        if (name.length() > 100) {
            throw new IllegalArgumentException("Name cannot exceed 100 characters");
        }

        validateKrakenPair(krakenPairName);

        if (marketCapRank == null || marketCapRank <= 0) {
            throw new IllegalArgumentException("Market cap rank must be positive: " + marketCapRank);
        }
    }

    private void validateCryptocurrencyForUpdate(Cryptocurrency crypto) {
        if (crypto == null) {
            throw new IllegalArgumentException("Cryptocurrency cannot be null");
        }

        if (crypto.getId() == null) {
            throw new IllegalArgumentException("Cryptocurrency ID cannot be null for update");
        }

        validateCryptocurrencyInput(crypto.getSymbol(), crypto.getName(),
                crypto.getKrakenPairName(), crypto.getMarketCapRank());

        if (crypto.getCurrentPrice() != null) {
            validatePrice(crypto.getCurrentPrice());
        }
    }

    // ===============================================
    // INNER CLASSES FOR MARKET DATA
    // ===============================================

    public static class MarketSummary {
        private final int totalCryptocurrencies;
        private final BigDecimal totalMarketValue;
        private final BigDecimal averagePrice;
        private final long positiveChanges;
        private final long negativeChanges;

        public MarketSummary(int totalCryptocurrencies, BigDecimal totalMarketValue,
                             BigDecimal averagePrice, long positiveChanges, long negativeChanges) {
            this.totalCryptocurrencies = totalCryptocurrencies;
            this.totalMarketValue = totalMarketValue;
            this.averagePrice = averagePrice;
            this.positiveChanges = positiveChanges;
            this.negativeChanges = negativeChanges;
        }

        // Getters
        public int getTotalCryptocurrencies() { return totalCryptocurrencies; }
        public BigDecimal getTotalMarketValue() { return totalMarketValue; }
        public BigDecimal getAveragePrice() { return averagePrice; }
        public long getPositiveChanges() { return positiveChanges; }
        public long getNegativeChanges() { return negativeChanges; }
    }
}