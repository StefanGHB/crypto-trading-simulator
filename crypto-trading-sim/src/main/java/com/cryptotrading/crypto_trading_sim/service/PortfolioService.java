package com.cryptotrading.crypto_trading_sim.service;

import com.cryptotrading.crypto_trading_sim.dao.UserDao;
import com.cryptotrading.crypto_trading_sim.dao.UserHoldingDao;
import com.cryptotrading.crypto_trading_sim.dao.CryptocurrencyDao;
import com.cryptotrading.crypto_trading_sim.dao.TransactionDao;
import com.cryptotrading.crypto_trading_sim.model.User;
import com.cryptotrading.crypto_trading_sim.model.UserHolding;
import com.cryptotrading.crypto_trading_sim.model.Cryptocurrency;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service layer for Portfolio-related business logic
 * Handles portfolio calculations, analysis, and reporting
 */
@Service
@Transactional
public class PortfolioService {

    @Autowired
    private UserDao userDao;

    @Autowired
    private UserHoldingDao userHoldingDao;

    @Autowired
    private CryptocurrencyDao cryptocurrencyDao;

    @Autowired
    private TransactionDao transactionDao;

    // ===============================================
    // PORTFOLIO OVERVIEW
    // ===============================================

    /**
     * Get complete portfolio overview for user
     */
    @Transactional(readOnly = true)
    public PortfolioOverview getPortfolioOverview(Long userId) {
        validateUserId(userId);

        User user = getUserById(userId);
        List<UserHoldingDao.HoldingWithDetails> holdings = userHoldingDao.findHoldingsWithDetailsByUserId(userId);

        // Calculate portfolio metrics
        BigDecimal totalPortfolioValue = calculateTotalPortfolioValue(holdings);
        BigDecimal totalInvested = calculateTotalInvested(holdings);
        BigDecimal totalUnrealizedPnL = calculateTotalUnrealizedPnL(holdings);
        BigDecimal totalAccountValue = user.getCurrentBalance().add(totalPortfolioValue);

        // Calculate overall performance vs initial balance
        BigDecimal overallPnL = totalAccountValue.subtract(user.getInitialBalance());
        BigDecimal overallPnLPercentage = user.getInitialBalance().compareTo(BigDecimal.ZERO) > 0 ?
                overallPnL.divide(user.getInitialBalance(), 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100")) :
                BigDecimal.ZERO;

        // Get realized P&L from transactions
        BigDecimal totalRealizedPnL = transactionDao.getTotalRealizedProfitLoss(userId);

        // Create portfolio positions
        List<PortfolioPosition> positions = holdings.stream()
                .map(this::convertToPortfolioPosition)
                .sorted((p1, p2) -> p2.getCurrentValue().compareTo(p1.getCurrentValue())) // Sort by value desc
                .collect(Collectors.toList());

        return new PortfolioOverview(
                user.getCurrentBalance(),
                totalPortfolioValue,
                totalAccountValue,
                user.getInitialBalance(),
                totalInvested,
                totalUnrealizedPnL,
                totalRealizedPnL,
                overallPnL,
                overallPnLPercentage,
                positions.size(),
                positions
        );
    }

    /**
     * Get simplified portfolio summary
     */
    @Transactional(readOnly = true)
    public PortfolioSummary getPortfolioSummary(Long userId) {
        validateUserId(userId);

        User user = getUserById(userId);
        BigDecimal portfolioValue = userHoldingDao.getTotalPortfolioValue(userId);
        BigDecimal totalInvested = userHoldingDao.getTotalInvestedByUser(userId);
        long holdingCount = userHoldingDao.getUserHoldingCount(userId);

        return new PortfolioSummary(
                user.getCurrentBalance(),
                portfolioValue,
                user.getCurrentBalance().add(portfolioValue),
                totalInvested,
                portfolioValue.subtract(totalInvested),
                holdingCount
        );
    }

    // ===============================================
    // PORTFOLIO POSITIONS
    // ===============================================

    /**
     * Get all portfolio positions for user
     */
    @Transactional(readOnly = true)
    public List<PortfolioPosition> getPortfolioPositions(Long userId) {
        validateUserId(userId);

        return userHoldingDao.findHoldingsWithDetailsByUserId(userId).stream()
                .map(this::convertToPortfolioPosition)
                .sorted((p1, p2) -> p2.getCurrentValue().compareTo(p1.getCurrentValue()))
                .collect(Collectors.toList());
    }

    /**
     * Get specific portfolio position
     */
    @Transactional(readOnly = true)
    public Optional<PortfolioPosition> getPortfolioPosition(Long userId, String cryptoSymbol) {
        validateUserId(userId);
        validateCryptoSymbol(cryptoSymbol);

        Long cryptoId = getCryptoIdBySymbol(cryptoSymbol);
        Optional<UserHolding> holdingOpt = userHoldingDao.findByUserAndCrypto(userId, cryptoId);

        if (holdingOpt.isEmpty() || holdingOpt.get().getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            return Optional.empty();
        }

        UserHolding holding = holdingOpt.get();
        Optional<Cryptocurrency> cryptoOpt = cryptocurrencyDao.findById(cryptoId);

        if (cryptoOpt.isEmpty()) {
            return Optional.empty();
        }

        Cryptocurrency crypto = cryptoOpt.get();
        return Optional.of(convertToPortfolioPosition(holding, crypto));
    }

    // ===============================================
    // PORTFOLIO ANALYTICS
    // ===============================================

    /**
     * Get portfolio allocation breakdown (by percentage)
     */
    @Transactional(readOnly = true)
    public List<PortfolioAllocation> getPortfolioAllocation(Long userId) {
        validateUserId(userId);

        List<UserHoldingDao.HoldingWithDetails> holdings = userHoldingDao.findHoldingsWithDetailsByUserId(userId);
        BigDecimal totalPortfolioValue = calculateTotalPortfolioValue(holdings);

        if (totalPortfolioValue.compareTo(BigDecimal.ZERO) <= 0) {
            return List.of(); // Empty portfolio
        }

        return holdings.stream()
                .map(holding -> {
                    BigDecimal currentValue = holding.getQuantity().multiply(holding.getCurrentPrice());
                    BigDecimal percentage = currentValue.divide(totalPortfolioValue, 4, RoundingMode.HALF_UP)
                            .multiply(new BigDecimal("100"));

                    return new PortfolioAllocation(
                            holding.getSymbol(),
                            holding.getCryptoName(),
                            currentValue,
                            percentage
                    );
                })
                .sorted((a1, a2) -> a2.getPercentage().compareTo(a1.getPercentage()))
                .collect(Collectors.toList());
    }

    /**
     * Get portfolio performance metrics
     */
    @Transactional(readOnly = true)
    public PortfolioPerformance getPortfolioPerformance(Long userId) {
        validateUserId(userId);

        PortfolioOverview overview = getPortfolioOverview(userId);
        List<PortfolioPosition> positions = overview.getPositions();

        // Calculate winners vs losers
        long winners = positions.stream()
                .filter(p -> p.getUnrealizedPnL().compareTo(BigDecimal.ZERO) > 0)
                .count();

        long losers = positions.stream()
                .filter(p -> p.getUnrealizedPnL().compareTo(BigDecimal.ZERO) < 0)
                .count();

        // Find best and worst performing positions
        Optional<PortfolioPosition> bestPerformer = positions.stream()
                .filter(p -> p.getUnrealizedPnLPercentage() != null)
                .max((p1, p2) -> p1.getUnrealizedPnLPercentage().compareTo(p2.getUnrealizedPnLPercentage()));

        Optional<PortfolioPosition> worstPerformer = positions.stream()
                .filter(p -> p.getUnrealizedPnLPercentage() != null)
                .min((p1, p2) -> p1.getUnrealizedPnLPercentage().compareTo(p2.getUnrealizedPnLPercentage()));


        int diversificationScore = Math.min(positions.size() * 10, 100);

        return new PortfolioPerformance(
                overview.getOverallPnL(),
                overview.getOverallPnLPercentage(),
                overview.getTotalUnrealizedPnL(),
                overview.getTotalRealizedPnL(),
                winners,
                losers,
                bestPerformer.orElse(null),
                worstPerformer.orElse(null),
                diversificationScore
        );
    }

    // ===============================================
    // PORTFOLIO UPDATES
    // ===============================================

    /**
     * Update all unrealized P&L for user's portfolio
     */
    public void updatePortfolioUnrealizedPnL(Long userId) {
        validateUserId(userId);
        userHoldingDao.updateAllUnrealizedProfitLoss(userId);
    }

    /**
     * Clean up zero-quantity holdings
     */
    public int cleanupZeroHoldings() {
        return userHoldingDao.deleteZeroQuantityHoldings();
    }

    // ===============================================
    // PORTFOLIO UTILITIES
    // ===============================================

    /**
     * Check if user has any holdings
     */
    @Transactional(readOnly = true)
    public boolean userHasHoldings(Long userId) {
        validateUserId(userId);
        return userHoldingDao.getUserHoldingCount(userId) > 0;
    }

    /**
     * Get user's quantity for specific cryptocurrency
     */
    @Transactional(readOnly = true)
    public BigDecimal getUserCryptoQuantity(Long userId, String cryptoSymbol) {
        validateUserId(userId);
        validateCryptoSymbol(cryptoSymbol);

        Long cryptoId = getCryptoIdBySymbol(cryptoSymbol);
        return userHoldingDao.getUserCryptoQuantity(userId, cryptoId);
    }

    /**
     * Check if user can sell specific quantity of cryptocurrency
     */
    @Transactional(readOnly = true)
    public boolean canSellQuantity(Long userId, String cryptoSymbol, BigDecimal quantity) {
        BigDecimal availableQuantity = getUserCryptoQuantity(userId, cryptoSymbol);
        return availableQuantity.compareTo(quantity) >= 0;
    }

    // ===============================================
    // HELPER METHODS
    // ===============================================

    private User getUserById(Long userId) {
        return userDao.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    }

    private Long getCryptoIdBySymbol(String symbol) {
        Long id = cryptocurrencyDao.getIdBySymbol(symbol.toUpperCase());
        if (id == null) {
            throw new IllegalArgumentException("Cryptocurrency not found: " + symbol);
        }
        return id;
    }

    private BigDecimal calculateTotalPortfolioValue(List<UserHoldingDao.HoldingWithDetails> holdings) {
        return holdings.stream()
                .map(holding -> holding.getQuantity().multiply(holding.getCurrentPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateTotalInvested(List<UserHoldingDao.HoldingWithDetails> holdings) {
        return holdings.stream()
                .map(UserHoldingDao.HoldingWithDetails::getTotalInvested)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateTotalUnrealizedPnL(List<UserHoldingDao.HoldingWithDetails> holdings) {
        return holdings.stream()
                .map(holding -> {
                    BigDecimal currentValue = holding.getQuantity().multiply(holding.getCurrentPrice());
                    return currentValue.subtract(holding.getTotalInvested());
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private PortfolioPosition convertToPortfolioPosition(UserHoldingDao.HoldingWithDetails holding) {
        BigDecimal currentValue = holding.getQuantity().multiply(holding.getCurrentPrice());
        BigDecimal unrealizedPnL = currentValue.subtract(holding.getTotalInvested());

        BigDecimal unrealizedPnLPercentage = holding.getTotalInvested().compareTo(BigDecimal.ZERO) > 0 ?
                unrealizedPnL.divide(holding.getTotalInvested(), 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100")) :
                BigDecimal.ZERO;

        return new PortfolioPosition(
                holding.getSymbol(),
                holding.getCryptoName(),
                holding.getQuantity(),
                holding.getAverageBuyPrice(),
                holding.getCurrentPrice(),
                currentValue,
                holding.getTotalInvested(),
                unrealizedPnL,
                unrealizedPnLPercentage,
                holding.getFirstPurchaseAt()
        );
    }

    private PortfolioPosition convertToPortfolioPosition(UserHolding holding, Cryptocurrency crypto) {
        BigDecimal currentValue = holding.getQuantity().multiply(crypto.getCurrentPrice());
        BigDecimal unrealizedPnL = currentValue.subtract(holding.getTotalInvested());

        BigDecimal unrealizedPnLPercentage = holding.getTotalInvested().compareTo(BigDecimal.ZERO) > 0 ?
                unrealizedPnL.divide(holding.getTotalInvested(), 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100")) :
                BigDecimal.ZERO;

        return new PortfolioPosition(
                crypto.getSymbol(),
                crypto.getName(),
                holding.getQuantity(),
                holding.getAverageBuyPrice(),
                crypto.getCurrentPrice(),
                currentValue,
                holding.getTotalInvested(),
                unrealizedPnL,
                unrealizedPnLPercentage,
                holding.getFirstPurchaseAt()
        );
    }

    // ===============================================
    // VALIDATION METHODS
    // ===============================================

    private void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("Invalid user ID: " + userId);
        }
    }

    private void validateCryptoSymbol(String symbol) {
        if (symbol == null || symbol.trim().isEmpty()) {
            throw new IllegalArgumentException("Cryptocurrency symbol cannot be null or empty");
        }
    }

    // ===============================================
    // INNER CLASSES FOR PORTFOLIO DATA
    // ===============================================

    public static class PortfolioOverview {
        private final BigDecimal cashBalance;
        private final BigDecimal totalPortfolioValue;
        private final BigDecimal totalAccountValue;
        private final BigDecimal initialBalance;
        private final BigDecimal totalInvested;
        private final BigDecimal totalUnrealizedPnL;
        private final BigDecimal totalRealizedPnL;
        private final BigDecimal overallPnL;
        private final BigDecimal overallPnLPercentage;
        private final int positionCount;
        private final List<PortfolioPosition> positions;

        public PortfolioOverview(BigDecimal cashBalance, BigDecimal totalPortfolioValue, BigDecimal totalAccountValue,
                                 BigDecimal initialBalance, BigDecimal totalInvested, BigDecimal totalUnrealizedPnL,
                                 BigDecimal totalRealizedPnL, BigDecimal overallPnL, BigDecimal overallPnLPercentage,
                                 int positionCount, List<PortfolioPosition> positions) {
            this.cashBalance = cashBalance;
            this.totalPortfolioValue = totalPortfolioValue;
            this.totalAccountValue = totalAccountValue;
            this.initialBalance = initialBalance;
            this.totalInvested = totalInvested;
            this.totalUnrealizedPnL = totalUnrealizedPnL;
            this.totalRealizedPnL = totalRealizedPnL;
            this.overallPnL = overallPnL;
            this.overallPnLPercentage = overallPnLPercentage;
            this.positionCount = positionCount;
            this.positions = positions;
        }

        // Getters
        public BigDecimal getCashBalance() { return cashBalance; }
        public BigDecimal getTotalPortfolioValue() { return totalPortfolioValue; }
        public BigDecimal getTotalAccountValue() { return totalAccountValue; }
        public BigDecimal getInitialBalance() { return initialBalance; }
        public BigDecimal getTotalInvested() { return totalInvested; }
        public BigDecimal getTotalUnrealizedPnL() { return totalUnrealizedPnL; }
        public BigDecimal getTotalRealizedPnL() { return totalRealizedPnL; }
        public BigDecimal getOverallPnL() { return overallPnL; }
        public BigDecimal getOverallPnLPercentage() { return overallPnLPercentage; }
        public int getPositionCount() { return positionCount; }
        public List<PortfolioPosition> getPositions() { return positions; }
    }

    public static class PortfolioSummary {
        private final BigDecimal cashBalance;
        private final BigDecimal portfolioValue;
        private final BigDecimal totalAccountValue;
        private final BigDecimal totalInvested;
        private final BigDecimal unrealizedPnL;
        private final long holdingCount;

        public PortfolioSummary(BigDecimal cashBalance, BigDecimal portfolioValue, BigDecimal totalAccountValue,
                                BigDecimal totalInvested, BigDecimal unrealizedPnL, long holdingCount) {
            this.cashBalance = cashBalance;
            this.portfolioValue = portfolioValue;
            this.totalAccountValue = totalAccountValue;
            this.totalInvested = totalInvested;
            this.unrealizedPnL = unrealizedPnL;
            this.holdingCount = holdingCount;
        }

        // Getters
        public BigDecimal getCashBalance() { return cashBalance; }
        public BigDecimal getPortfolioValue() { return portfolioValue; }
        public BigDecimal getTotalAccountValue() { return totalAccountValue; }
        public BigDecimal getTotalInvested() { return totalInvested; }
        public BigDecimal getUnrealizedPnL() { return unrealizedPnL; }
        public long getHoldingCount() { return holdingCount; }
    }

    public static class PortfolioPosition {
        private final String symbol;
        private final String name;
        private final BigDecimal quantity;
        private final BigDecimal averageBuyPrice;
        private final BigDecimal currentPrice;
        private final BigDecimal currentValue;
        private final BigDecimal totalInvested;
        private final BigDecimal unrealizedPnL;
        private final BigDecimal unrealizedPnLPercentage;
        private final java.time.LocalDateTime firstPurchaseDate;

        public PortfolioPosition(String symbol, String name, BigDecimal quantity, BigDecimal averageBuyPrice,
                                 BigDecimal currentPrice, BigDecimal currentValue, BigDecimal totalInvested,
                                 BigDecimal unrealizedPnL, BigDecimal unrealizedPnLPercentage,
                                 java.time.LocalDateTime firstPurchaseDate) {
            this.symbol = symbol;
            this.name = name;
            this.quantity = quantity;
            this.averageBuyPrice = averageBuyPrice;
            this.currentPrice = currentPrice;
            this.currentValue = currentValue;
            this.totalInvested = totalInvested;
            this.unrealizedPnL = unrealizedPnL;
            this.unrealizedPnLPercentage = unrealizedPnLPercentage;
            this.firstPurchaseDate = firstPurchaseDate;
        }

        // Getters
        public String getSymbol() { return symbol; }
        public String getName() { return name; }
        public BigDecimal getQuantity() { return quantity; }
        public BigDecimal getAverageBuyPrice() { return averageBuyPrice; }
        public BigDecimal getCurrentPrice() { return currentPrice; }
        public BigDecimal getCurrentValue() { return currentValue; }
        public BigDecimal getTotalInvested() { return totalInvested; }
        public BigDecimal getUnrealizedPnL() { return unrealizedPnL; }
        public BigDecimal getUnrealizedPnLPercentage() { return unrealizedPnLPercentage; }
        public java.time.LocalDateTime getFirstPurchaseDate() { return firstPurchaseDate; }
    }

    public static class PortfolioAllocation {
        private final String symbol;
        private final String name;
        private final BigDecimal value;
        private final BigDecimal percentage;

        public PortfolioAllocation(String symbol, String name, BigDecimal value, BigDecimal percentage) {
            this.symbol = symbol;
            this.name = name;
            this.value = value;
            this.percentage = percentage;
        }

        // Getters
        public String getSymbol() { return symbol; }
        public String getName() { return name; }
        public BigDecimal getValue() { return value; }
        public BigDecimal getPercentage() { return percentage; }
    }

    public static class PortfolioPerformance {
        private final BigDecimal overallPnL;
        private final BigDecimal overallPnLPercentage;
        private final BigDecimal totalUnrealizedPnL;
        private final BigDecimal totalRealizedPnL;
        private final long winners;
        private final long losers;
        private final PortfolioPosition bestPerformer;
        private final PortfolioPosition worstPerformer;
        private final int diversificationScore;

        public PortfolioPerformance(BigDecimal overallPnL, BigDecimal overallPnLPercentage,
                                    BigDecimal totalUnrealizedPnL, BigDecimal totalRealizedPnL,
                                    long winners, long losers, PortfolioPosition bestPerformer,
                                    PortfolioPosition worstPerformer, int diversificationScore) {
            this.overallPnL = overallPnL;
            this.overallPnLPercentage = overallPnLPercentage;
            this.totalUnrealizedPnL = totalUnrealizedPnL;
            this.totalRealizedPnL = totalRealizedPnL;
            this.winners = winners;
            this.losers = losers;
            this.bestPerformer = bestPerformer;
            this.worstPerformer = worstPerformer;
            this.diversificationScore = diversificationScore;
        }

        // Getters
        public BigDecimal getOverallPnL() { return overallPnL; }
        public BigDecimal getOverallPnLPercentage() { return overallPnLPercentage; }
        public BigDecimal getTotalUnrealizedPnL() { return totalUnrealizedPnL; }
        public BigDecimal getTotalRealizedPnL() { return totalRealizedPnL; }
        public long getWinners() { return winners; }
        public long getLosers() { return losers; }
        public PortfolioPosition getBestPerformer() { return bestPerformer; }
        public PortfolioPosition getWorstPerformer() { return worstPerformer; }
        public int getDiversificationScore() { return diversificationScore; }
    }
}