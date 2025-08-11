package com.cryptotrading.crypto_trading_sim.service;

import com.cryptotrading.crypto_trading_sim.dao.UserDao;
import com.cryptotrading.crypto_trading_sim.dao.CryptocurrencyDao;
import com.cryptotrading.crypto_trading_sim.dao.UserHoldingDao;
import com.cryptotrading.crypto_trading_sim.dao.TransactionDao;
import com.cryptotrading.crypto_trading_sim.model.User;
import com.cryptotrading.crypto_trading_sim.model.Cryptocurrency;
import com.cryptotrading.crypto_trading_sim.model.UserHolding;
import com.cryptotrading.crypto_trading_sim.model.Transaction;
import com.cryptotrading.crypto_trading_sim.model.enums.TransactionType;
import com.cryptotrading.crypto_trading_sim.model.enums.TransactionStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;


@Service
@Transactional
public class TradingService {

    @Autowired
    private UserDao userDao;

    @Autowired
    private CryptocurrencyDao cryptocurrencyDao;

    @Autowired
    private UserHoldingDao userHoldingDao;

    @Autowired
    private TransactionDao transactionDao;

    @Value("${trading.fee.percentage:0.10}")
    private BigDecimal tradingFeePercentage;

    @Value("${trading.max.decimal.places:8}")
    private int maxDecimalPlaces;

    @Value("${trading.min.transaction.amount:0.01}")
    private BigDecimal minTransactionAmount;

    @Value("${trading.max.transaction.amount:100000}")
    private BigDecimal maxTransactionAmount;

    // ===============================================
    // BUY OPERATIONS
    // ===============================================

    /**
     * Execute buy order by specifying USD amount to spend
     */
    public TradingResult buyByAmount(Long userId, String cryptoSymbol, BigDecimal amountToSpend) {
        validateTradingInputs(userId, cryptoSymbol, amountToSpend);

        // Get current price
        BigDecimal currentPrice = getCurrentPriceForTrading(cryptoSymbol);

        // Calculate quantity to buy
        BigDecimal fees = calculateFees(amountToSpend);
        BigDecimal amountAfterFees = amountToSpend.subtract(fees);
        BigDecimal quantity = amountAfterFees.divide(currentPrice, maxDecimalPlaces, RoundingMode.DOWN);

        return executeBuyOrder(userId, cryptoSymbol, quantity, currentPrice, fees);
    }

    /**
     * Execute buy order by specifying quantity to buy
     */
    public TradingResult buyByQuantity(Long userId, String cryptoSymbol, BigDecimal quantity) {
        validateTradingQuantity(quantity);
        validateUserAndCrypto(userId, cryptoSymbol);

        // Get current price
        BigDecimal currentPrice = getCurrentPriceForTrading(cryptoSymbol);

        // Calculate total amount needed
        BigDecimal subtotal = quantity.multiply(currentPrice);
        BigDecimal fees = calculateFees(subtotal);
        BigDecimal totalAmount = subtotal.add(fees);

        validateTransactionAmount(totalAmount);

        return executeBuyOrder(userId, cryptoSymbol, quantity, currentPrice, fees);
    }


    private TradingResult executeBuyOrder(Long userId, String cryptoSymbol, BigDecimal quantity,
                                          BigDecimal currentPrice, BigDecimal fees) {
        try {
            // Calculate amounts
            BigDecimal subtotal = quantity.multiply(currentPrice);
            BigDecimal totalAmount = subtotal.add(fees);

            // Validate user can afford this
            User user = getUserForTrading(userId);
            if (!user.canAfford(totalAmount)) {
                return new TradingResult(false, "Insufficient balance. Required: $" + totalAmount +
                        ", Available: $" + user.getCurrentBalance(), null, null);
            }

            // Get cryptocurrency
            Cryptocurrency crypto = getCryptocurrencyForTrading(cryptoSymbol);

            // Create transaction record
            Transaction transaction = new Transaction(userId, crypto.getId(), TransactionType.BUY,
                    quantity, currentPrice, user.getCurrentBalance());
            transaction.setFees(fees);
            transaction.setTotalAmount(totalAmount);
            transaction.setBalanceAfter(user.getCurrentBalance().subtract(totalAmount));

            // Save transaction
            transactionDao.save(transaction);

            // Update user balance
            userDao.updateBalance(userId, transaction.getBalanceAfter());

            // 🔧 FIXED: Update or create user holding with proper cost basis including fees
            updateUserHoldingForPurchase(userId, crypto.getId(), quantity, currentPrice, fees, totalAmount);

            return new TradingResult(true, "Buy order executed successfully", transaction,
                    buildTradingSummary(transaction, crypto, TransactionType.BUY));

        } catch (Exception e) {
            throw new RuntimeException("Failed to execute buy order: " + e.getMessage(), e);
        }
    }

    // ===============================================
    // SELL OPERATIONS
    // ===============================================

    /**
     * Execute sell order by specifying USD amount to receive
     */
    public TradingResult sellByAmount(Long userId, String cryptoSymbol, BigDecimal amountToReceive) {
        validateTradingInputs(userId, cryptoSymbol, amountToReceive);

        // Get current price
        BigDecimal currentPrice = getCurrentPriceForTrading(cryptoSymbol);

        // Calculate fees and net amount
        BigDecimal fees = calculateFees(amountToReceive);
        BigDecimal grossAmount = amountToReceive.add(fees);
        BigDecimal quantityToSell = grossAmount.divide(currentPrice, maxDecimalPlaces, RoundingMode.UP);

        return executeSellOrder(userId, cryptoSymbol, quantityToSell, currentPrice, fees);
    }

    /**
     * Execute sell order by specifying quantity to sell
     */
    public TradingResult sellByQuantity(Long userId, String cryptoSymbol, BigDecimal quantity) {
        validateTradingQuantity(quantity);
        validateUserAndCrypto(userId, cryptoSymbol);

        // Get current price
        BigDecimal currentPrice = getCurrentPriceForTrading(cryptoSymbol);

        // Calculate amounts
        BigDecimal grossAmount = quantity.multiply(currentPrice);
        BigDecimal fees = calculateFees(grossAmount);

        validateTransactionAmount(grossAmount);

        return executeSellOrder(userId, cryptoSymbol, quantity, currentPrice, fees);
    }

    /**
     * Sell all holdings of a specific cryptocurrency
     */
    public TradingResult sellAll(Long userId, String cryptoSymbol) {
        validateUserAndCrypto(userId, cryptoSymbol);

        // Get user's current holding
        Cryptocurrency crypto = getCryptocurrencyForTrading(cryptoSymbol);
        Optional<UserHolding> holdingOpt = userHoldingDao.findByUserAndCrypto(userId, crypto.getId());

        if (holdingOpt.isEmpty() || holdingOpt.get().getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            return new TradingResult(false, "No holdings found for " + cryptoSymbol, null, null);
        }

        BigDecimal quantityToSell = holdingOpt.get().getQuantity();
        return sellByQuantity(userId, cryptoSymbol, quantityToSell);
    }


    private TradingResult executeSellOrder(Long userId, String cryptoSymbol, BigDecimal quantity,
                                           BigDecimal currentPrice, BigDecimal fees) {
        try {
            // Get cryptocurrency
            Cryptocurrency crypto = getCryptocurrencyForTrading(cryptoSymbol);

            // Validate user has enough holdings
            Optional<UserHolding> holdingOpt = userHoldingDao.findByUserAndCrypto(userId, crypto.getId());
            if (holdingOpt.isEmpty() || holdingOpt.get().getQuantity().compareTo(quantity) < 0) {
                return new TradingResult(false, "Insufficient " + cryptoSymbol + " holdings. Required: " +
                        quantity + ", Available: " + (holdingOpt.isPresent() ? holdingOpt.get().getQuantity() : "0"),
                        null, null);
            }

            UserHolding holding = holdingOpt.get();
            User user = getUserForTrading(userId);


            BigDecimal grossAmount = quantity.multiply(currentPrice);
            BigDecimal netAmount = grossAmount.subtract(fees);


            BigDecimal averageCostPerUnit = holding.getTotalInvested().divide(holding.getQuantity(), maxDecimalPlaces, RoundingMode.HALF_UP);
            BigDecimal soldCostBasis = quantity.multiply(averageCostPerUnit);


            BigDecimal realizedPnL = grossAmount.subtract(soldCostBasis).subtract(fees);


            System.out.println("=== PERFECT SELL CALCULATION ===");
            System.out.println("Crypto: " + cryptoSymbol);
            System.out.println("Quantity to sell: " + quantity);
            System.out.println("Current price: " + currentPrice);
            System.out.println("Gross amount (revenue): " + grossAmount);
            System.out.println("Sell fees: " + fees);
            System.out.println("Holdings - Total Invested: " + holding.getTotalInvested());
            System.out.println("Holdings - Total Quantity: " + holding.getQuantity());
            System.out.println("Average cost per unit: " + averageCostPerUnit);
            System.out.println("Cost basis of sold portion: " + soldCostBasis);
            System.out.println("Realized P&L: " + realizedPnL);
            System.out.println("Net amount to user: " + netAmount);
            System.out.println("===============================");

            // Create transaction record
            Transaction transaction = new Transaction(userId, crypto.getId(), TransactionType.SELL,
                    quantity, currentPrice, user.getCurrentBalance());
            transaction.setFees(fees);
            transaction.setTotalAmount(grossAmount);
            transaction.setRealizedProfitLoss(realizedPnL);
            transaction.setBalanceAfter(user.getCurrentBalance().add(netAmount));

            // Save transaction
            transactionDao.save(transaction);

            // Update user balance
            userDao.updateBalance(userId, transaction.getBalanceAfter());

            // Update user holding
            updateUserHoldingForSale(holding, quantity);

            return new TradingResult(true, "Sell order executed successfully", transaction,
                    buildTradingSummary(transaction, crypto, TransactionType.SELL));

        } catch (Exception e) {
            throw new RuntimeException("Failed to execute sell order: " + e.getMessage(), e);
        }
    }

    // ===============================================
    // TRADING UTILITIES
    // ===============================================

    /**
     * Get current market price for trading
     */
    @Transactional(readOnly = true)
    public BigDecimal getCurrentPrice(String cryptoSymbol) {
        return getCurrentPriceForTrading(cryptoSymbol);
    }

    /**
     * Calculate trading fees
     */
    public BigDecimal calculateFees(BigDecimal amount) {
        if (tradingFeePercentage.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return amount.multiply(tradingFeePercentage)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }

    /**
     * Get trading quote for buy order
     */
    @Transactional(readOnly = true)
    public TradingQuote getBuyQuote(Long userId, String cryptoSymbol, BigDecimal amountToSpend) {
        validateTradingInputs(userId, cryptoSymbol, amountToSpend);

        BigDecimal currentPrice = getCurrentPriceForTrading(cryptoSymbol);
        BigDecimal fees = calculateFees(amountToSpend);
        BigDecimal amountAfterFees = amountToSpend.subtract(fees);
        BigDecimal quantity = amountAfterFees.divide(currentPrice, maxDecimalPlaces, RoundingMode.DOWN);

        return new TradingQuote(TransactionType.BUY, quantity, currentPrice, amountToSpend, fees, amountAfterFees);
    }

    /**
     * Get trading quote for sell order
     */
    @Transactional(readOnly = true)
    public TradingQuote getSellQuote(Long userId, String cryptoSymbol, BigDecimal quantity) {
        validateTradingQuantity(quantity);
        validateUserAndCrypto(userId, cryptoSymbol);

        BigDecimal currentPrice = getCurrentPriceForTrading(cryptoSymbol);
        BigDecimal grossAmount = quantity.multiply(currentPrice);
        BigDecimal fees = calculateFees(grossAmount);
        BigDecimal netAmount = grossAmount.subtract(fees);

        return new TradingQuote(TransactionType.SELL, quantity, currentPrice, grossAmount, fees, netAmount);
    }

    // ===============================================
    // HELPER METHODS
    // ===============================================

    private BigDecimal getCurrentPriceForTrading(String cryptoSymbol) {
        BigDecimal currentPrice = cryptocurrencyDao.getCurrentPrice(cryptoSymbol.toUpperCase());
        if (currentPrice == null || currentPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Invalid or unavailable price for " + cryptoSymbol);
        }
        return currentPrice;
    }

    private User getUserForTrading(Long userId) {
        Optional<User> userOpt = userDao.findById(userId);
        if (userOpt.isEmpty() || !userOpt.get().isActive()) {
            throw new IllegalArgumentException("User not found or inactive: " + userId);
        }
        return userOpt.get();
    }

    private Cryptocurrency getCryptocurrencyForTrading(String symbol) {
        Optional<Cryptocurrency> cryptoOpt = cryptocurrencyDao.findBySymbol(symbol.toUpperCase());
        if (cryptoOpt.isEmpty() || !cryptoOpt.get().isActive()) {
            throw new IllegalArgumentException("Cryptocurrency not found or inactive: " + symbol);
        }
        return cryptoOpt.get();
    }


    private void updateUserHoldingForPurchase(Long userId, Long cryptoId, BigDecimal quantity,
                                              BigDecimal price, BigDecimal fees, BigDecimal totalAmount) {
        Optional<UserHolding> existingHolding = userHoldingDao.findByUserAndCrypto(userId, cryptoId);

        if (existingHolding.isPresent()) {
            // Update existing holding with new purchase including fees
            UserHolding holding = existingHolding.get();
            holding.addPurchaseWithFees(quantity, price, fees, totalAmount);
            userHoldingDao.update(holding);
        } else {
            // Create new holding with proper cost basis including fees
            BigDecimal trueCostPerUnit = totalAmount.divide(quantity, maxDecimalPlaces, RoundingMode.HALF_UP);
            UserHolding newHolding = new UserHolding(userId, cryptoId, quantity, trueCostPerUnit);
            newHolding.setTotalInvested(totalAmount); // Include fees in total invested
            userHoldingDao.save(newHolding);
        }
    }

    /**
     * 🔧 PERFECT: Update user holding for sale with proper cost basis reduction
     */
    private void updateUserHoldingForSale(UserHolding holding, BigDecimal quantitySold) {
        holding.reduceSale(quantitySold);

        if (holding.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            // Remove holding if quantity is zero or negative
            userHoldingDao.delete(holding.getId());
        } else {
            // Update holding
            userHoldingDao.update(holding);
        }
    }

    private TradingSummary buildTradingSummary(Transaction transaction, Cryptocurrency crypto, TransactionType type) {
        return new TradingSummary(
                type,
                crypto.getSymbol(),
                crypto.getName(),
                transaction.getQuantity(),
                transaction.getPricePerUnit(),
                transaction.getTotalAmount(),
                transaction.getFees(),
                transaction.getRealizedProfitLoss(),
                transaction.getBalanceAfter()
        );
    }

    // ===============================================
    // VALIDATION METHODS
    // ===============================================

    private void validateTradingInputs(Long userId, String cryptoSymbol, BigDecimal amount) {
        validateUserAndCrypto(userId, cryptoSymbol);
        validateTransactionAmount(amount);
    }

    private void validateUserAndCrypto(Long userId, String cryptoSymbol) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("Invalid user ID: " + userId);
        }

        if (cryptoSymbol == null || cryptoSymbol.trim().isEmpty()) {
            throw new IllegalArgumentException("Cryptocurrency symbol cannot be null or empty");
        }
    }

    private void validateTransactionAmount(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Transaction amount cannot be null");
        }

        if (amount.compareTo(minTransactionAmount) < 0) {
            throw new IllegalArgumentException("Transaction amount too small. Minimum: $" + minTransactionAmount);
        }

        if (amount.compareTo(maxTransactionAmount) > 0) {
            throw new IllegalArgumentException("Transaction amount too large. Maximum: $" + maxTransactionAmount);
        }
    }

    private void validateTradingQuantity(BigDecimal quantity) {
        if (quantity == null) {
            throw new IllegalArgumentException("Quantity cannot be null");
        }

        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be positive: " + quantity);
        }

        if (quantity.scale() > maxDecimalPlaces) {
            throw new IllegalArgumentException("Quantity has too many decimal places. Maximum: " + maxDecimalPlaces);
        }
    }

    // ===============================================
    // INNER CLASSES FOR TRADING DATA
    // ===============================================

    public static class TradingResult {
        private final boolean success;
        private final String message;
        private final Transaction transaction;
        private final TradingSummary summary;

        public TradingResult(boolean success, String message, Transaction transaction, TradingSummary summary) {
            this.success = success;
            this.message = message;
            this.transaction = transaction;
            this.summary = summary;
        }

        // Getters
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public Transaction getTransaction() { return transaction; }
        public TradingSummary getSummary() { return summary; }
    }

    public static class TradingQuote {
        private final TransactionType type;
        private final BigDecimal quantity;
        private final BigDecimal pricePerUnit;
        private final BigDecimal totalAmount;
        private final BigDecimal fees;
        private final BigDecimal netAmount;

        public TradingQuote(TransactionType type, BigDecimal quantity, BigDecimal pricePerUnit,
                            BigDecimal totalAmount, BigDecimal fees, BigDecimal netAmount) {
            this.type = type;
            this.quantity = quantity;
            this.pricePerUnit = pricePerUnit;
            this.totalAmount = totalAmount;
            this.fees = fees;
            this.netAmount = netAmount;
        }

        // Getters
        public TransactionType getType() { return type; }
        public BigDecimal getQuantity() { return quantity; }
        public BigDecimal getPricePerUnit() { return pricePerUnit; }
        public BigDecimal getTotalAmount() { return totalAmount; }
        public BigDecimal getFees() { return fees; }
        public BigDecimal getNetAmount() { return netAmount; }
    }

    public static class TradingSummary {
        private final TransactionType type;
        private final String cryptoSymbol;
        private final String cryptoName;
        private final BigDecimal quantity;
        private final BigDecimal pricePerUnit;
        private final BigDecimal totalAmount;
        private final BigDecimal fees;
        private final BigDecimal realizedPnL;
        private final BigDecimal newBalance;

        public TradingSummary(TransactionType type, String cryptoSymbol, String cryptoName,
                              BigDecimal quantity, BigDecimal pricePerUnit, BigDecimal totalAmount,
                              BigDecimal fees, BigDecimal realizedPnL, BigDecimal newBalance) {
            this.type = type;
            this.cryptoSymbol = cryptoSymbol;
            this.cryptoName = cryptoName;
            this.quantity = quantity;
            this.pricePerUnit = pricePerUnit;
            this.totalAmount = totalAmount;
            this.fees = fees;
            this.realizedPnL = realizedPnL;
            this.newBalance = newBalance;
        }

        // Getters
        public TransactionType getType() { return type; }
        public String getCryptoSymbol() { return cryptoSymbol; }
        public String getCryptoName() { return cryptoName; }
        public BigDecimal getQuantity() { return quantity; }
        public BigDecimal getPricePerUnit() { return pricePerUnit; }
        public BigDecimal getTotalAmount() { return totalAmount; }
        public BigDecimal getFees() { return fees; }
        public BigDecimal getRealizedPnL() { return realizedPnL; }
        public BigDecimal getNewBalance() { return newBalance; }
    }
}