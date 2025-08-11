package com.cryptotrading.crypto_trading_sim.service;

import com.cryptotrading.crypto_trading_sim.dao.TransactionDao;
import com.cryptotrading.crypto_trading_sim.dao.UserDao;
import com.cryptotrading.crypto_trading_sim.dao.CryptocurrencyDao;
import com.cryptotrading.crypto_trading_sim.model.Transaction;
import com.cryptotrading.crypto_trading_sim.model.enums.TransactionType;
import com.cryptotrading.crypto_trading_sim.model.enums.TransactionStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service layer for Transaction-related business logic
 * Handles transaction history, analytics, and reporting
 */
@Service
@Transactional
public class TransactionService {

    @Autowired
    private TransactionDao transactionDao;

    @Autowired
    private UserDao userDao;

    @Autowired
    private CryptocurrencyDao cryptocurrencyDao;

    // ===============================================
    // TRANSACTION RETRIEVAL
    // ===============================================

    /**
     * Get transaction by ID
     */
    @Transactional(readOnly = true)
    public Optional<Transaction> getTransactionById(Long transactionId) {
        validateTransactionId(transactionId);
        return transactionDao.findById(transactionId);
    }

    /**
     * Get all transactions for a user (most recent first)
     */
    @Transactional(readOnly = true)
    public List<Transaction> getUserTransactions(Long userId) {
        validateUserId(userId);
        return transactionDao.findByUserId(userId);
    }

    /**
     * Get transactions with crypto details for a user
     */
    @Transactional(readOnly = true)
    public List<TransactionDao.TransactionWithDetails> getUserTransactionsWithDetails(Long userId) {
        validateUserId(userId);
        return transactionDao.findTransactionsWithDetailsByUserId(userId);
    }

    /**
     * Get recent transactions for a user (limited count)
     */
    @Transactional(readOnly = true)
    public List<Transaction> getRecentUserTransactions(Long userId, int limit) {
        validateUserId(userId);
        validateLimit(limit);
        return transactionDao.findRecentTransactionsByUserId(userId, limit);
    }

    /**
     * Get transactions for a specific cryptocurrency
     */
    @Transactional(readOnly = true)
    public List<Transaction> getUserTransactionsForCrypto(Long userId, String cryptoSymbol) {
        validateUserId(userId);
        validateCryptoSymbol(cryptoSymbol);

        Long cryptoId = getCryptoIdBySymbol(cryptoSymbol);
        return transactionDao.findByUserAndCrypto(userId, cryptoId);
    }

    /**
     * Get transactions by type (BUY or SELL)
     */
    @Transactional(readOnly = true)
    public List<Transaction> getUserTransactionsByType(Long userId, TransactionType type) {
        validateUserId(userId);
        validateTransactionType(type);
        return transactionDao.findByUserAndType(userId, type);
    }

    /**
     * Get transactions within date range
     */
    @Transactional(readOnly = true)
    public List<Transaction> getTransactionsByDateRange(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        validateUserId(userId);
        validateDateRange(startDate, endDate);
        return transactionDao.findTransactionsByDateRange(userId, startDate, endDate);
    }

    // ===============================================
    // TRANSACTION ANALYTICS
    // ===============================================

    /**
     * Get comprehensive transaction statistics for user
     */
    @Transactional(readOnly = true)
    public TransactionAnalytics getTransactionAnalytics(Long userId) {
        validateUserId(userId);

        TransactionDao.TransactionStatistics stats = transactionDao.getTransactionStatistics(userId);
        List<Transaction> allTransactions = getUserTransactions(userId);

        // Calculate additional metrics
        BigDecimal averageTransactionSize = BigDecimal.ZERO;
        if (stats.getTotalTransactions() > 0) {
            BigDecimal totalVolume = stats.getTotalBought().add(stats.getTotalSold());
            averageTransactionSize = totalVolume.divide(
                    new BigDecimal(stats.getTotalTransactions()), 2, BigDecimal.ROUND_HALF_UP);
        }

        // Find most traded cryptocurrency
        String mostTradedCrypto = findMostTradedCryptocurrency(userId);

        // Calculate trading frequency (transactions per day since first transaction)
        double tradingFrequency = calculateTradingFrequency(allTransactions);

        return new TransactionAnalytics(
                stats.getTotalTransactions(),
                stats.getBuyCount(),
                stats.getSellCount(),
                stats.getTotalBought(),
                stats.getTotalSold(),
                stats.getTotalRealizedPnl(),
                averageTransactionSize,
                mostTradedCrypto,
                tradingFrequency
        );
    }

    /**
     * Get profit/loss breakdown by cryptocurrency
     */
    @Transactional(readOnly = true)
    public List<CryptoProfitLoss> getProfitLossBreakdown(Long userId) {
        validateUserId(userId);

        return getUserTransactionsWithDetails(userId).stream()
                .filter(t -> t.getTransactionType() == TransactionType.SELL && t.getRealizedProfitLoss() != null)
                .collect(Collectors.groupingBy(
                        t -> new CryptoInfo(t.getSymbol(), t.getCryptoName()),
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                t -> t.getRealizedProfitLoss(),
                                BigDecimal::add
                        )
                ))
                .entrySet().stream()
                .map(entry -> new CryptoProfitLoss(
                        entry.getKey().getSymbol(),
                        entry.getKey().getName(),
                        entry.getValue()
                ))
                .sorted((c1, c2) -> c2.getProfitLoss().compareTo(c1.getProfitLoss()))
                .collect(Collectors.toList());
    }

    /**
     * Get transaction summary for a specific period
     */
    @Transactional(readOnly = true)
    public TransactionPeriodSummary getTransactionSummaryForPeriod(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        validateUserId(userId);
        validateDateRange(startDate, endDate);

        List<Transaction> periodTransactions = getTransactionsByDateRange(userId, startDate, endDate);

        long buyCount = periodTransactions.stream()
                .filter(t -> t.getTransactionType() == TransactionType.BUY)
                .count();

        long sellCount = periodTransactions.stream()
                .filter(t -> t.getTransactionType() == TransactionType.SELL)
                .count();

        BigDecimal totalBought = periodTransactions.stream()
                .filter(t -> t.getTransactionType() == TransactionType.BUY)
                .map(Transaction::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalSold = periodTransactions.stream()
                .filter(t -> t.getTransactionType() == TransactionType.SELL)
                .map(Transaction::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalFees = periodTransactions.stream()
                .map(t -> t.getFees() != null ? t.getFees() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRealizedPnL = periodTransactions.stream()
                .filter(t -> t.getTransactionType() == TransactionType.SELL && t.getRealizedProfitLoss() != null)
                .map(Transaction::getRealizedProfitLoss)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new TransactionPeriodSummary(
                startDate, endDate,
                periodTransactions.size(),
                buyCount, sellCount,
                totalBought, totalSold,
                totalFees, totalRealizedPnL
        );
    }

    // ===============================================
    // TRANSACTION HISTORY EXPORT
    // ===============================================

    /**
     * Get transaction history formatted for export/display
     */
    @Transactional(readOnly = true)
    public List<TransactionHistoryEntry> getTransactionHistoryForExport(Long userId) {
        validateUserId(userId);

        return getUserTransactionsWithDetails(userId).stream()
                .map(this::convertToHistoryEntry)
                .collect(Collectors.toList());
    }

    /**
     * Get transaction history for specific cryptocurrency
     */
    @Transactional(readOnly = true)
    public List<TransactionHistoryEntry> getTransactionHistoryForCrypto(Long userId, String cryptoSymbol) {
        validateUserId(userId);
        validateCryptoSymbol(cryptoSymbol);

        Long cryptoId = getCryptoIdBySymbol(cryptoSymbol);

        return transactionDao.findByUserAndCrypto(userId, cryptoId).stream()
                .map(transaction -> {
                    // Get crypto details
                    Optional<com.cryptotrading.crypto_trading_sim.model.Cryptocurrency> cryptoOpt =
                            cryptocurrencyDao.findById(transaction.getCryptoId());
                    String symbol = cryptoOpt.map(c -> c.getSymbol()).orElse("UNKNOWN");
                    String name = cryptoOpt.map(c -> c.getName()).orElse("Unknown");

                    return convertToHistoryEntry(transaction, symbol, name);
                })
                .collect(Collectors.toList());
    }

    // ===============================================
    // TRANSACTION VALIDATION & UPDATES
    // ===============================================

    /**
     * Update transaction status
     */
    public boolean updateTransactionStatus(Long transactionId, TransactionStatus status) {
        validateTransactionId(transactionId);
        validateTransactionStatus(status);

        return transactionDao.updateStatus(transactionId, status);
    }

    /**
     * Get transaction count for user
     */
    @Transactional(readOnly = true)
    public long getUserTransactionCount(Long userId) {
        validateUserId(userId);
        return transactionDao.getUserTransactionCount(userId);
    }

    /**
     * Check if user has any transactions
     */
    @Transactional(readOnly = true)
    public boolean userHasTransactions(Long userId) {
        return getUserTransactionCount(userId) > 0;
    }

    // ===============================================
    // HELPER METHODS
    // ===============================================

    private String findMostTradedCryptocurrency(Long userId) {
        List<TransactionDao.TransactionWithDetails> transactions = getUserTransactionsWithDetails(userId);

        if (transactions.isEmpty()) {
            return "None";
        }

        return transactions.stream()
                .collect(Collectors.groupingBy(
                        TransactionDao.TransactionWithDetails::getSymbol,
                        Collectors.counting()
                ))
                .entrySet().stream()
                .max((e1, e2) -> e1.getValue().compareTo(e2.getValue()))
                .map(entry -> entry.getKey())
                .orElse("None");
    }

    private double calculateTradingFrequency(List<Transaction> transactions) {
        if (transactions.size() < 2) {
            return 0.0;
        }

        // Sort by date (oldest first)
        transactions.sort((t1, t2) -> t1.getCreatedAt().compareTo(t2.getCreatedAt()));

        LocalDateTime firstTransaction = transactions.get(0).getCreatedAt();
        LocalDateTime lastTransaction = transactions.get(transactions.size() - 1).getCreatedAt();

        long daysBetween = java.time.Duration.between(firstTransaction, lastTransaction).toDays();
        if (daysBetween == 0) {
            daysBetween = 1; // Avoid division by zero
        }

        return (double) transactions.size() / daysBetween;
    }

    private TransactionHistoryEntry convertToHistoryEntry(TransactionDao.TransactionWithDetails transaction) {
        return convertToHistoryEntry(transaction, transaction.getSymbol(), transaction.getCryptoName());
    }

    private TransactionHistoryEntry convertToHistoryEntry(Transaction transaction, String symbol, String name) {
        return new TransactionHistoryEntry(
                transaction.getId(),
                transaction.getTransactionType(),
                symbol,
                name,
                transaction.getQuantity(),
                transaction.getPricePerUnit(),
                transaction.getTotalAmount(),
                transaction.getFees() != null ? transaction.getFees() : BigDecimal.ZERO,
                transaction.getRealizedProfitLoss(),
                transaction.getBalanceAfter(),
                transaction.getTransactionStatus(),
                transaction.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        );
    }

    private Long getCryptoIdBySymbol(String symbol) {
        return cryptocurrencyDao.getIdBySymbol(symbol.toUpperCase());
    }

    // ===============================================
    // VALIDATION METHODS
    // ===============================================

    private void validateTransactionId(Long transactionId) {
        if (transactionId == null || transactionId <= 0) {
            throw new IllegalArgumentException("Invalid transaction ID: " + transactionId);
        }
    }

    private void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("Invalid user ID: " + userId);
        }

        if (!userDao.findById(userId).isPresent()) {
            throw new IllegalArgumentException("User not found: " + userId);
        }
    }

    private void validateCryptoSymbol(String symbol) {
        if (symbol == null || symbol.trim().isEmpty()) {
            throw new IllegalArgumentException("Cryptocurrency symbol cannot be null or empty");
        }
    }

    private void validateTransactionType(TransactionType type) {
        if (type == null) {
            throw new IllegalArgumentException("Transaction type cannot be null");
        }
    }

    private void validateTransactionStatus(TransactionStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Transaction status cannot be null");
        }
    }

    private void validateDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start date and end date cannot be null");
        }

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }

        if (startDate.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Start date cannot be in the future");
        }
    }

    private void validateLimit(int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be positive: " + limit);
        }

        if (limit > 1000) {
            throw new IllegalArgumentException("Limit cannot exceed 1000: " + limit);
        }
    }

    // ===============================================
    // INNER CLASSES FOR TRANSACTION DATA
    // ===============================================

    public static class TransactionAnalytics {
        private final long totalTransactions;
        private final long buyCount;
        private final long sellCount;
        private final BigDecimal totalBought;
        private final BigDecimal totalSold;
        private final BigDecimal totalRealizedPnL;
        private final BigDecimal averageTransactionSize;
        private final String mostTradedCryptocurrency;
        private final double tradingFrequency;

        public TransactionAnalytics(long totalTransactions, long buyCount, long sellCount,
                                    BigDecimal totalBought, BigDecimal totalSold, BigDecimal totalRealizedPnL,
                                    BigDecimal averageTransactionSize, String mostTradedCryptocurrency, double tradingFrequency) {
            this.totalTransactions = totalTransactions;
            this.buyCount = buyCount;
            this.sellCount = sellCount;
            this.totalBought = totalBought;
            this.totalSold = totalSold;
            this.totalRealizedPnL = totalRealizedPnL;
            this.averageTransactionSize = averageTransactionSize;
            this.mostTradedCryptocurrency = mostTradedCryptocurrency;
            this.tradingFrequency = tradingFrequency;
        }

        // Getters
        public long getTotalTransactions() { return totalTransactions; }
        public long getBuyCount() { return buyCount; }
        public long getSellCount() { return sellCount; }
        public BigDecimal getTotalBought() { return totalBought; }
        public BigDecimal getTotalSold() { return totalSold; }
        public BigDecimal getTotalRealizedPnL() { return totalRealizedPnL; }
        public BigDecimal getAverageTransactionSize() { return averageTransactionSize; }
        public String getMostTradedCryptocurrency() { return mostTradedCryptocurrency; }
        public double getTradingFrequency() { return tradingFrequency; }
    }

    public static class CryptoProfitLoss {
        private final String symbol;
        private final String name;
        private final BigDecimal profitLoss;

        public CryptoProfitLoss(String symbol, String name, BigDecimal profitLoss) {
            this.symbol = symbol;
            this.name = name;
            this.profitLoss = profitLoss;
        }

        // Getters
        public String getSymbol() { return symbol; }
        public String getName() { return name; }
        public BigDecimal getProfitLoss() { return profitLoss; }
    }

    public static class TransactionPeriodSummary {
        private final LocalDateTime startDate;
        private final LocalDateTime endDate;
        private final long totalTransactions;
        private final long buyCount;
        private final long sellCount;
        private final BigDecimal totalBought;
        private final BigDecimal totalSold;
        private final BigDecimal totalFees;
        private final BigDecimal totalRealizedPnL;

        public TransactionPeriodSummary(LocalDateTime startDate, LocalDateTime endDate,
                                        long totalTransactions, long buyCount, long sellCount,
                                        BigDecimal totalBought, BigDecimal totalSold,
                                        BigDecimal totalFees, BigDecimal totalRealizedPnL) {
            this.startDate = startDate;
            this.endDate = endDate;
            this.totalTransactions = totalTransactions;
            this.buyCount = buyCount;
            this.sellCount = sellCount;
            this.totalBought = totalBought;
            this.totalSold = totalSold;
            this.totalFees = totalFees;
            this.totalRealizedPnL = totalRealizedPnL;
        }

        // Getters
        public LocalDateTime getStartDate() { return startDate; }
        public LocalDateTime getEndDate() { return endDate; }
        public long getTotalTransactions() { return totalTransactions; }
        public long getBuyCount() { return buyCount; }
        public long getSellCount() { return sellCount; }
        public BigDecimal getTotalBought() { return totalBought; }
        public BigDecimal getTotalSold() { return totalSold; }
        public BigDecimal getTotalFees() { return totalFees; }
        public BigDecimal getTotalRealizedPnL() { return totalRealizedPnL; }
    }

    public static class TransactionHistoryEntry {
        private final Long id;
        private final TransactionType type;
        private final String cryptoSymbol;
        private final String cryptoName;
        private final BigDecimal quantity;
        private final BigDecimal pricePerUnit;
        private final BigDecimal totalAmount;
        private final BigDecimal fees;
        private final BigDecimal realizedPnL;
        private final BigDecimal balanceAfter;
        private final TransactionStatus status;
        private final String dateTime;

        public TransactionHistoryEntry(Long id, TransactionType type, String cryptoSymbol, String cryptoName,
                                       BigDecimal quantity, BigDecimal pricePerUnit, BigDecimal totalAmount,
                                       BigDecimal fees, BigDecimal realizedPnL, BigDecimal balanceAfter,
                                       TransactionStatus status, String dateTime) {
            this.id = id;
            this.type = type;
            this.cryptoSymbol = cryptoSymbol;
            this.cryptoName = cryptoName;
            this.quantity = quantity;
            this.pricePerUnit = pricePerUnit;
            this.totalAmount = totalAmount;
            this.fees = fees;
            this.realizedPnL = realizedPnL;
            this.balanceAfter = balanceAfter;
            this.status = status;
            this.dateTime = dateTime;
        }

        // Getters
        public Long getId() { return id; }
        public TransactionType getType() { return type; }
        public String getCryptoSymbol() { return cryptoSymbol; }
        public String getCryptoName() { return cryptoName; }
        public BigDecimal getQuantity() { return quantity; }
        public BigDecimal getPricePerUnit() { return pricePerUnit; }
        public BigDecimal getTotalAmount() { return totalAmount; }
        public BigDecimal getFees() { return fees; }
        public BigDecimal getRealizedPnL() { return realizedPnL; }
        public BigDecimal getBalanceAfter() { return balanceAfter; }
        public TransactionStatus getStatus() { return status; }
        public String getDateTime() { return dateTime; }
    }

    // Helper class for crypto grouping
    private static class CryptoInfo {
        private final String symbol;
        private final String name;

        public CryptoInfo(String symbol, String name) {
            this.symbol = symbol;
            this.name = name;
        }

        public String getSymbol() { return symbol; }
        public String getName() { return name; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CryptoInfo)) return false;
            CryptoInfo that = (CryptoInfo) o;
            return symbol.equals(that.symbol);
        }

        @Override
        public int hashCode() {
            return symbol.hashCode();
        }
    }
}