package com.cryptotrading.crypto_trading_sim.utils;

import com.cryptotrading.crypto_trading_sim.model.*;
import com.cryptotrading.crypto_trading_sim.model.enums.TransactionStatus;
import com.cryptotrading.crypto_trading_sim.model.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Test data builder for creating test objects with sensible defaults
 */
public class TestDataBuilder {

    // ===============================================
    // USER BUILDERS
    // ===============================================

    public static User createTestUser() {
        return new User(TestConstants.TEST_USERNAME, TestConstants.TEST_EMAIL, TestConstants.DEFAULT_INITIAL_BALANCE);
    }

    public static User createTestUserWithId(Long id) {
        User user = createTestUser();
        user.setId(id);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }

    public static User createUserWithBalance(BigDecimal balance) {
        User user = createTestUser();
        user.setCurrentBalance(balance);
        return user;
    }

    // ===============================================
    // CRYPTOCURRENCY BUILDERS
    // ===============================================

    public static Cryptocurrency createBitcoin() {
        Cryptocurrency btc = new Cryptocurrency(TestConstants.BTC_SYMBOL, TestConstants.BTC_NAME,
                TestConstants.BTC_KRAKEN_PAIR, 1);
        btc.setCurrentPrice(TestConstants.BTC_PRICE);
        btc.setPriceChange24h(TestConstants.PRICE_CHANGE_24H);
        btc.setPriceChangePercent24h(TestConstants.PRICE_CHANGE_PERCENT_24H);
        return btc;
    }

    public static Cryptocurrency createEthereum() {
        Cryptocurrency eth = new Cryptocurrency(TestConstants.ETH_SYMBOL, TestConstants.ETH_NAME,
                TestConstants.ETH_KRAKEN_PAIR, 2);
        eth.setCurrentPrice(TestConstants.ETH_PRICE);
        eth.setPriceChange24h(new BigDecimal("100.00"));
        eth.setPriceChangePercent24h(new BigDecimal("3.45"));
        return eth;
    }

    public static Cryptocurrency createCryptocurrencyWithId(Long id) {
        Cryptocurrency crypto = createBitcoin();
        crypto.setId(id);
        crypto.setCreatedAt(LocalDateTime.now());
        crypto.setUpdatedAt(LocalDateTime.now());
        return crypto;
    }

    // ===============================================
    // TRANSACTION BUILDERS
    // ===============================================

    public static Transaction createBuyTransaction(Long userId, Long cryptoId) {
        Transaction transaction = new Transaction(userId, cryptoId, TransactionType.BUY,
                TestConstants.TRADE_QUANTITY, TestConstants.BTC_PRICE, TestConstants.DEFAULT_INITIAL_BALANCE);
        transaction.setId(TestConstants.TEST_TRANSACTION_ID);
        transaction.setFees(new BigDecimal("1.00"));
        return transaction;
    }

    public static Transaction createSellTransaction(Long userId, Long cryptoId) {
        Transaction transaction = new Transaction(userId, cryptoId, TransactionType.SELL,
                TestConstants.TRADE_QUANTITY, TestConstants.BTC_PRICE, TestConstants.UPDATED_BALANCE);
        transaction.setId(TestConstants.TEST_TRANSACTION_ID);
        transaction.setFees(new BigDecimal("1.00"));
        transaction.setRealizedProfitLoss(TestConstants.REALIZED_PNL);
        return transaction;
    }

    public static Transaction createTransactionWithStatus(TransactionStatus status) {
        Transaction transaction = createBuyTransaction(TestConstants.TEST_USER_ID, TestConstants.TEST_CRYPTO_ID);
        transaction.setTransactionStatus(status);
        return transaction;
    }

    // ===============================================
    // USER HOLDING BUILDERS
    // ===============================================

    public static UserHolding createUserHolding(Long userId, Long cryptoId) {
        UserHolding holding = new UserHolding(userId, cryptoId, TestConstants.TRADE_QUANTITY, TestConstants.BTC_PRICE);
        holding.setId(TestConstants.TEST_HOLDING_ID);
        holding.setTotalInvested(TestConstants.TRADE_QUANTITY.multiply(TestConstants.BTC_PRICE));
        holding.setUnrealizedProfitLoss(TestConstants.UNREALIZED_PNL);
        return holding;
    }

    public static UserHolding createHoldingWithQuantity(BigDecimal quantity) {
        UserHolding holding = createUserHolding(TestConstants.TEST_USER_ID, TestConstants.TEST_CRYPTO_ID);
        holding.setQuantity(quantity);
        holding.setTotalInvested(quantity.multiply(TestConstants.BTC_PRICE));
        return holding;
    }

    public static UserHolding createProfitableHolding() {
        UserHolding holding = createUserHolding(TestConstants.TEST_USER_ID, TestConstants.TEST_CRYPTO_ID);
        holding.setAverageBuyPrice(new BigDecimal("45000.00")); // Bought cheaper
        holding.setUnrealizedProfitLoss(new BigDecimal("100.00")); // Positive P&L
        return holding;
    }

    public static UserHolding createLosingHolding() {
        UserHolding holding = createUserHolding(TestConstants.TEST_USER_ID, TestConstants.TEST_CRYPTO_ID);
        holding.setAverageBuyPrice(new BigDecimal("55000.00")); // Bought expensive
        holding.setUnrealizedProfitLoss(new BigDecimal("-100.00")); // Negative P&L
        return holding;
    }

    // ===============================================
    // HELPER METHODS
    // ===============================================

    public static BigDecimal calculateExpectedFees(BigDecimal amount) {
        return amount.multiply(TestConstants.TRADING_FEE_PERCENTAGE)
                .divide(new BigDecimal("100"), 2, BigDecimal.ROUND_HALF_UP);
    }

    public static BigDecimal calculateTotalCost(BigDecimal quantity, BigDecimal price) {
        BigDecimal subtotal = quantity.multiply(price);
        BigDecimal fees = calculateExpectedFees(subtotal);
        return subtotal.add(fees);
    }

    public static BigDecimal calculateNetAmount(BigDecimal quantity, BigDecimal price) {
        BigDecimal grossAmount = quantity.multiply(price);
        BigDecimal fees = calculateExpectedFees(grossAmount);
        return grossAmount.subtract(fees);
    }
}