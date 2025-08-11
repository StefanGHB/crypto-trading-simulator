package com.cryptotrading.crypto_trading_sim.integration;

import com.cryptotrading.crypto_trading_sim.service.TradingService;
import com.cryptotrading.crypto_trading_sim.service.UserService;
import com.cryptotrading.crypto_trading_sim.service.CryptocurrencyService;
import com.cryptotrading.crypto_trading_sim.service.PortfolioService;
import com.cryptotrading.crypto_trading_sim.model.User;
import com.cryptotrading.crypto_trading_sim.model.Cryptocurrency;
import com.cryptotrading.crypto_trading_sim.utils.TestConstants;
import com.cryptotrading.crypto_trading_sim.utils.TestDataBuilder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Trading workflow - Testing complete trading scenarios
 * 🔧 FIXED: BigDecimal precision issues resolved
 */
@SpringBootTest
@TestPropertySource(properties = {
        "trading.fee.percentage=0.10",
        "trading.default.initial.balance=10000.00"
})
@Transactional
class TradingIntegrationTest {

    @Autowired
    private TradingService tradingService;

    @Autowired
    private UserService userService;

    @Autowired
    private CryptocurrencyService cryptocurrencyService;

    @Autowired
    private PortfolioService portfolioService;

    private User testUser;
    private Cryptocurrency testCrypto;

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = userService.createUser("integrationuser", "integration@test.com");

        // Check if crypto exists before creating to avoid duplicate errors
        Optional<Cryptocurrency> existingCrypto = cryptocurrencyService.getCryptocurrencyBySymbol(TestConstants.BTC_SYMBOL);
        if (existingCrypto.isPresent()) {
            testCrypto = existingCrypto.get();
        } else {
            testCrypto = cryptocurrencyService.createCryptocurrency(
                    TestConstants.BTC_SYMBOL, TestConstants.BTC_NAME,
                    TestConstants.BTC_KRAKEN_PAIR, 1);
        }

        // Set initial price
        cryptocurrencyService.updatePrice(TestConstants.BTC_SYMBOL, TestConstants.BTC_PRICE);
    }

    // ===============================================
    // COMPLETE BUY WORKFLOW TESTS
    // ===============================================

    @Test
    void testCompleteBuyWorkflow() {
        // Given
        BigDecimal amountToSpend = new BigDecimal("1000.00");
        BigDecimal initialBalance = testUser.getCurrentBalance();

        // When - Execute buy order
        TradingService.TradingResult buyResult = tradingService.buyByAmount(
                testUser.getId(), TestConstants.BTC_SYMBOL, amountToSpend);

        // Then - Verify buy was successful
        assertTrue(buyResult.isSuccess());
        assertNotNull(buyResult.getTransaction());
        assertNotNull(buyResult.getSummary());

        // Verify user balance was updated
        BigDecimal currentBalance = userService.getCurrentBalance(testUser.getId());
        assertTrue(currentBalance.compareTo(initialBalance) < 0); // Balance should decrease

        // Verify portfolio was updated
        PortfolioService.PortfolioSummary portfolio = portfolioService.getPortfolioSummary(testUser.getId());
        assertTrue(portfolio.getPortfolioValue().compareTo(BigDecimal.ZERO) > 0); // Should have holdings
        assertEquals(1L, portfolio.getHoldingCount()); // Should have 1 position

        // Verify user has cryptocurrency quantity
        BigDecimal quantity = portfolioService.getUserCryptoQuantity(testUser.getId(), TestConstants.BTC_SYMBOL);
        assertTrue(quantity.compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void testCompleteSellWorkflow() {
        // Given - First buy some cryptocurrency
        BigDecimal buyAmount = new BigDecimal("1000.00");
        TradingService.TradingResult buyResult = tradingService.buyByAmount(
                testUser.getId(), TestConstants.BTC_SYMBOL, buyAmount);
        assertTrue(buyResult.isSuccess());

        // Get the quantity we bought
        BigDecimal boughtQuantity = buyResult.getTransaction().getQuantity();
        BigDecimal sellQuantity = boughtQuantity.divide(new BigDecimal("2")); // Sell half

        BigDecimal balanceBeforeSell = userService.getCurrentBalance(testUser.getId());

        // When - Execute sell order
        TradingService.TradingResult sellResult = tradingService.sellByQuantity(
                testUser.getId(), TestConstants.BTC_SYMBOL, sellQuantity);

        // Then - Verify sell was successful
        assertTrue(sellResult.isSuccess());
        assertNotNull(sellResult.getTransaction());
        assertNotNull(sellResult.getTransaction().getRealizedProfitLoss());

        // Verify user balance increased
        BigDecimal balanceAfterSell = userService.getCurrentBalance(testUser.getId());
        assertTrue(balanceAfterSell.compareTo(balanceBeforeSell) > 0);

        // Verify portfolio was updated
        BigDecimal remainingQuantity = portfolioService.getUserCryptoQuantity(testUser.getId(), TestConstants.BTC_SYMBOL);
        // 🔧 FIXED: Use compareTo instead of assertEquals for BigDecimal
        assertEquals(0, boughtQuantity.subtract(sellQuantity).stripTrailingZeros().compareTo(remainingQuantity.stripTrailingZeros()));
    }

    @Test
    void testSellAllWorkflow() {
        // Given - First buy some cryptocurrency
        BigDecimal buyAmount = new BigDecimal("2000.00");
        TradingService.TradingResult buyResult = tradingService.buyByAmount(
                testUser.getId(), TestConstants.BTC_SYMBOL, buyAmount);
        assertTrue(buyResult.isSuccess());

        BigDecimal balanceBeforeSell = userService.getCurrentBalance(testUser.getId());

        // When - Sell all holdings
        TradingService.TradingResult sellAllResult = tradingService.sellAll(
                testUser.getId(), TestConstants.BTC_SYMBOL);

        // Then - Verify sell all was successful
        assertTrue(sellAllResult.isSuccess());

        // Verify user balance increased
        BigDecimal balanceAfterSell = userService.getCurrentBalance(testUser.getId());
        assertTrue(balanceAfterSell.compareTo(balanceBeforeSell) > 0);

        // Verify no holdings remain
        BigDecimal remainingQuantity = portfolioService.getUserCryptoQuantity(testUser.getId(), TestConstants.BTC_SYMBOL);
        // 🔧 FIXED: Use stripTrailingZeros for proper comparison
        assertEquals(0, remainingQuantity.stripTrailingZeros().compareTo(BigDecimal.ZERO));

        // Verify portfolio is empty or very close to zero
        PortfolioService.PortfolioSummary portfolio = portfolioService.getPortfolioSummary(testUser.getId());
        assertTrue(portfolio.getPortfolioValue().abs().compareTo(new BigDecimal("0.01")) < 0); // Allow tiny precision errors
        assertEquals(0L, portfolio.getHoldingCount());
    }

    // ===============================================
    // PORTFOLIO UPDATES AFTER TRADES
    // ===============================================

    @Test
    void testPortfolioUpdatesAfterTrade() {
        // Given - Initial empty portfolio
        PortfolioService.PortfolioSummary initialPortfolio = portfolioService.getPortfolioSummary(testUser.getId());
        // 🔧 FIXED: Use stripTrailingZeros for comparison
        assertEquals(0, initialPortfolio.getPortfolioValue().stripTrailingZeros().compareTo(BigDecimal.ZERO));
        assertEquals(0L, initialPortfolio.getHoldingCount());

        // When - Execute buy order
        BigDecimal buyAmount = new BigDecimal("1500.00");
        TradingService.TradingResult buyResult = tradingService.buyByAmount(
                testUser.getId(), TestConstants.BTC_SYMBOL, buyAmount);
        assertTrue(buyResult.isSuccess());

        // Then - Verify portfolio was updated
        PortfolioService.PortfolioSummary updatedPortfolio = portfolioService.getPortfolioSummary(testUser.getId());
        assertTrue(updatedPortfolio.getPortfolioValue().compareTo(BigDecimal.ZERO) > 0);
        assertEquals(1L, updatedPortfolio.getHoldingCount());

        // Verify portfolio overview
        PortfolioService.PortfolioOverview overview = portfolioService.getPortfolioOverview(testUser.getId());
        assertEquals(1, overview.getPositionCount());
        assertFalse(overview.getPositions().isEmpty());

        PortfolioService.PortfolioPosition position = overview.getPositions().get(0);
        assertEquals(TestConstants.BTC_SYMBOL, position.getSymbol());
        assertTrue(position.getQuantity().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(position.getCurrentValue().compareTo(BigDecimal.ZERO) > 0);
    }

    // ===============================================
    // MULTIPLE TRADES SCENARIO
    // ===============================================

    @Test
    void testMultipleTradesScenario() {
        // Scenario: Buy -> Sell -> Buy again -> Sell all

        // Trade 1: Initial buy
        BigDecimal firstBuyAmount = new BigDecimal("1000.00");
        TradingService.TradingResult firstBuy = tradingService.buyByAmount(
                testUser.getId(), TestConstants.BTC_SYMBOL, firstBuyAmount);
        assertTrue(firstBuy.isSuccess());

        BigDecimal firstQuantity = portfolioService.getUserCryptoQuantity(testUser.getId(), TestConstants.BTC_SYMBOL);
        assertTrue(firstQuantity.compareTo(BigDecimal.ZERO) > 0);

        // Trade 2: Partial sell
        BigDecimal sellQuantity = firstQuantity.divide(new BigDecimal("2"));
        TradingService.TradingResult firstSell = tradingService.sellByQuantity(
                testUser.getId(), TestConstants.BTC_SYMBOL, sellQuantity);
        assertTrue(firstSell.isSuccess());

        BigDecimal remainingAfterSell = portfolioService.getUserCryptoQuantity(testUser.getId(), TestConstants.BTC_SYMBOL);
        // 🔧 FIXED: Use stripTrailingZeros for proper comparison
        assertEquals(0, firstQuantity.subtract(sellQuantity).stripTrailingZeros().compareTo(remainingAfterSell.stripTrailingZeros()));

        // Trade 3: Second buy (accumulating position)
        BigDecimal secondBuyAmount = new BigDecimal("500.00");
        TradingService.TradingResult secondBuy = tradingService.buyByAmount(
                testUser.getId(), TestConstants.BTC_SYMBOL, secondBuyAmount);
        assertTrue(secondBuy.isSuccess());

        BigDecimal totalAfterSecondBuy = portfolioService.getUserCryptoQuantity(testUser.getId(), TestConstants.BTC_SYMBOL);
        assertTrue(totalAfterSecondBuy.compareTo(remainingAfterSell) > 0);

        // Trade 4: Sell all
        TradingService.TradingResult sellAll = tradingService.sellAll(testUser.getId(), TestConstants.BTC_SYMBOL);
        assertTrue(sellAll.isSuccess());

        // Final verification
        BigDecimal finalQuantity = portfolioService.getUserCryptoQuantity(testUser.getId(), TestConstants.BTC_SYMBOL);
        // 🔧 FIXED: Allow for tiny precision errors
        assertTrue(finalQuantity.abs().compareTo(new BigDecimal("0.00000001")) < 0);

        // Verify portfolio is clean
        PortfolioService.PortfolioSummary finalPortfolio = portfolioService.getPortfolioSummary(testUser.getId());
        // 🔧 FIXED: Allow for tiny precision errors
        assertTrue(finalPortfolio.getPortfolioValue().abs().compareTo(new BigDecimal("0.01")) < 0);
        assertEquals(0L, finalPortfolio.getHoldingCount());
    }

    // ===============================================
    // ERROR SCENARIOS
    // ===============================================

    @Test
    void testInsufficientBalanceScenario() {
        // Given - Try to spend more than available balance
        BigDecimal userBalance = userService.getCurrentBalance(testUser.getId());
        BigDecimal excessiveAmount = userBalance.add(new BigDecimal("1000.00"));

        // When
        TradingService.TradingResult result = tradingService.buyByAmount(
                testUser.getId(), TestConstants.BTC_SYMBOL, excessiveAmount);

        // Then
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Insufficient balance"));

        // Verify no changes were made
        BigDecimal balanceAfterFailedTrade = userService.getCurrentBalance(testUser.getId());
        assertEquals(0, userBalance.compareTo(balanceAfterFailedTrade));

        PortfolioService.PortfolioSummary portfolio = portfolioService.getPortfolioSummary(testUser.getId());
        // 🔧 FIXED: Use stripTrailingZeros for comparison
        assertEquals(0, portfolio.getPortfolioValue().stripTrailingZeros().compareTo(BigDecimal.ZERO));
    }

    @Test
    void testInsufficientHoldingsScenario() {
        // Given - Try to sell more than available holdings
        BigDecimal excessiveQuantity = new BigDecimal("10.0"); // Large amount

        // When
        TradingService.TradingResult result = tradingService.sellByQuantity(
                testUser.getId(), TestConstants.BTC_SYMBOL, excessiveQuantity);

        // Then
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Insufficient"));

        // Verify no changes were made
        BigDecimal quantity = portfolioService.getUserCryptoQuantity(testUser.getId(), TestConstants.BTC_SYMBOL);
        assertEquals(0, quantity.compareTo(BigDecimal.ZERO));
    }

    // ===============================================
    // PROFIT/LOSS CALCULATION TESTS
    // ===============================================

    @Test
    void testProfitLossCalculation() {
        // Given - Buy at current price
        BigDecimal buyAmount = new BigDecimal("1000.00");
        TradingService.TradingResult buyResult = tradingService.buyByAmount(
                testUser.getId(), TestConstants.BTC_SYMBOL, buyAmount);
        assertTrue(buyResult.isSuccess());

        BigDecimal boughtQuantity = buyResult.getTransaction().getQuantity();

        // When - Price changes and we sell
        BigDecimal newPrice = TestConstants.BTC_PRICE.multiply(new BigDecimal("1.1")); // 10% increase
        cryptocurrencyService.updatePrice(TestConstants.BTC_SYMBOL, newPrice);

        TradingService.TradingResult sellResult = tradingService.sellAll(testUser.getId(), TestConstants.BTC_SYMBOL);
        assertTrue(sellResult.isSuccess());

        // Then - Should have positive realized P&L
        BigDecimal realizedPnL = sellResult.getTransaction().getRealizedProfitLoss();
        assertNotNull(realizedPnL);
        assertTrue(realizedPnL.compareTo(BigDecimal.ZERO) > 0); // Should be profitable due to price increase
    }
}