package com.cryptotrading.crypto_trading_sim.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Test constants for consistent test data across all test classes
 */
public class TestConstants {

    // User Test Data
    public static final String TEST_USERNAME = "testuser";
    public static final String TEST_EMAIL = "test@example.com";
    public static final BigDecimal DEFAULT_INITIAL_BALANCE = new BigDecimal("10000.00").setScale(2, RoundingMode.HALF_UP);
    public static final BigDecimal UPDATED_BALANCE = new BigDecimal("8500.00").setScale(2, RoundingMode.HALF_UP);

    // Cryptocurrency Test Data
    public static final String BTC_SYMBOL = "BTC";
    public static final String ETH_SYMBOL = "ETH";
    public static final String BTC_NAME = "Bitcoin";
    public static final String ETH_NAME = "Ethereum";
    public static final String BTC_KRAKEN_PAIR = "BTC/USD";
    public static final String ETH_KRAKEN_PAIR = "ETH/USD";

    // Price Test Data - Consistent precision
    public static final BigDecimal BTC_PRICE = new BigDecimal("50000.00").setScale(8, RoundingMode.HALF_UP);
    public static final BigDecimal ETH_PRICE = new BigDecimal("3000.00").setScale(8, RoundingMode.HALF_UP);
    public static final BigDecimal UPDATED_BTC_PRICE = new BigDecimal("51000.00").setScale(8, RoundingMode.HALF_UP);
    public static final BigDecimal PRICE_CHANGE_24H = new BigDecimal("1000.00").setScale(4, RoundingMode.HALF_UP);
    public static final BigDecimal PRICE_CHANGE_PERCENT_24H = new BigDecimal("2.00").setScale(4, RoundingMode.HALF_UP);

    // Trading Test Data - 🔧 FIXED: Use 8 decimal places for proper validation
    public static final BigDecimal TRADE_AMOUNT = new BigDecimal("1000.00").setScale(2, RoundingMode.HALF_UP);
    public static final BigDecimal TRADE_QUANTITY = new BigDecimal("0.01998000").setScale(8, RoundingMode.HALF_UP);
    public static final BigDecimal SMALL_QUANTITY = new BigDecimal("0.00999000").setScale(8, RoundingMode.HALF_UP);
    public static final BigDecimal LARGE_QUANTITY = new BigDecimal("1.00000000").setScale(8, RoundingMode.HALF_UP);
    public static final BigDecimal TRADING_FEE_PERCENTAGE = new BigDecimal("0.10").setScale(2, RoundingMode.HALF_UP);

    // Database Test Data
    public static final Long TEST_USER_ID = 1L;
    public static final Long TEST_CRYPTO_ID = 1L;
    public static final Long TEST_TRANSACTION_ID = 1L;
    public static final Long TEST_HOLDING_ID = 1L;

    // Validation Test Data
    public static final BigDecimal NEGATIVE_AMOUNT = new BigDecimal("-100.00").setScale(2, RoundingMode.HALF_UP);
    public static final BigDecimal ZERO_AMOUNT = BigDecimal.ZERO.setScale(8, RoundingMode.HALF_UP);
    public static final BigDecimal VERY_LARGE_AMOUNT = new BigDecimal("999999999.00").setScale(2, RoundingMode.HALF_UP);

    // Error Messages
    public static final String INSUFFICIENT_BALANCE_ERROR = "Insufficient balance";
    public static final String INSUFFICIENT_HOLDINGS_ERROR = "Insufficient holdings";
    public static final String INVALID_AMOUNT_ERROR = "Invalid amount";
    public static final String USER_NOT_FOUND_ERROR = "User not found";
    public static final String CRYPTO_NOT_FOUND_ERROR = "Cryptocurrency not found";

    // Portfolio Test Data
    public static final BigDecimal PORTFOLIO_VALUE = new BigDecimal("5000.00").setScale(2, RoundingMode.HALF_UP);
    public static final BigDecimal TOTAL_INVESTED = new BigDecimal("4800.00").setScale(2, RoundingMode.HALF_UP);
    public static final BigDecimal UNREALIZED_PNL = new BigDecimal("200.00").setScale(2, RoundingMode.HALF_UP);
    public static final BigDecimal REALIZED_PNL = new BigDecimal("150.00").setScale(2, RoundingMode.HALF_UP);

    private TestConstants() {
        // Utility class - no instantiation
    }
}