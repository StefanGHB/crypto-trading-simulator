package com.cryptotrading.crypto_trading_sim.service;

import com.cryptotrading.crypto_trading_sim.dao.UserDao;
import com.cryptotrading.crypto_trading_sim.dao.UserHoldingDao;
import com.cryptotrading.crypto_trading_sim.dao.CryptocurrencyDao;
import com.cryptotrading.crypto_trading_sim.dao.TransactionDao;
import com.cryptotrading.crypto_trading_sim.model.User;
import com.cryptotrading.crypto_trading_sim.model.UserHolding;
import com.cryptotrading.crypto_trading_sim.model.Cryptocurrency;
import com.cryptotrading.crypto_trading_sim.utils.TestConstants;
import com.cryptotrading.crypto_trading_sim.utils.TestDataBuilder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PortfolioService - Testing portfolio calculations and analysis
 * 🔧 FIXED: Removed unnecessary stubbings and improved mock setup
 */
@ExtendWith(MockitoExtension.class)
class PortfolioServiceTest {

    @Mock
    private UserDao userDao;

    @Mock
    private UserHoldingDao userHoldingDao;

    @Mock
    private CryptocurrencyDao cryptocurrencyDao;

    @Mock
    private TransactionDao transactionDao;

    @InjectMocks
    private PortfolioService portfolioService;

    private User testUser;
    private UserHolding testHolding;
    private Cryptocurrency testCrypto;

    @BeforeEach
    void setUp() {
        testUser = TestDataBuilder.createTestUserWithId(TestConstants.TEST_USER_ID);
        testHolding = TestDataBuilder.createUserHolding(TestConstants.TEST_USER_ID, TestConstants.TEST_CRYPTO_ID);
        testCrypto = TestDataBuilder.createCryptocurrencyWithId(TestConstants.TEST_CRYPTO_ID);
    }

    // 🔧 FIXED: Create specific mocks only when needed for each test
    private List<UserHoldingDao.HoldingWithDetails> createBasicHoldingsWithDetails() {
        UserHoldingDao.HoldingWithDetails holding1 = mock(UserHoldingDao.HoldingWithDetails.class);

        when(holding1.getSymbol()).thenReturn(TestConstants.BTC_SYMBOL);
        when(holding1.getCryptoName()).thenReturn(TestConstants.BTC_NAME);
        when(holding1.getQuantity()).thenReturn(TestConstants.TRADE_QUANTITY);
        when(holding1.getCurrentPrice()).thenReturn(TestConstants.BTC_PRICE);
        when(holding1.getTotalInvested()).thenReturn(new BigDecimal("900.00"));

        return Arrays.asList(holding1);
    }

    // ===============================================
    // PORTFOLIO OVERVIEW TESTS
    // ===============================================

    @Test
    void testGetPortfolioOverview_WithHoldings() {
        // Given
        List<UserHoldingDao.HoldingWithDetails> testHoldings = createBasicHoldingsWithDetails();

        when(userDao.findById(TestConstants.TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(userHoldingDao.findHoldingsWithDetailsByUserId(TestConstants.TEST_USER_ID)).thenReturn(testHoldings);
        when(transactionDao.getTotalRealizedProfitLoss(TestConstants.TEST_USER_ID)).thenReturn(TestConstants.REALIZED_PNL);

        // When
        PortfolioService.PortfolioOverview overview = portfolioService.getPortfolioOverview(TestConstants.TEST_USER_ID);

        // Then
        assertNotNull(overview);
        assertEquals(testUser.getCurrentBalance(), overview.getCashBalance());
        assertTrue(overview.getTotalPortfolioValue().compareTo(BigDecimal.ZERO) > 0);
        assertEquals(1, overview.getPositionCount());
        assertNotNull(overview.getPositions());
        assertEquals(1, overview.getPositions().size());
        assertEquals(TestConstants.REALIZED_PNL, overview.getTotalRealizedPnL());
    }

    @Test
    void testGetPortfolioOverview_EmptyPortfolio() {
        // Given
        when(userDao.findById(TestConstants.TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(userHoldingDao.findHoldingsWithDetailsByUserId(TestConstants.TEST_USER_ID)).thenReturn(Collections.emptyList());
        when(transactionDao.getTotalRealizedProfitLoss(TestConstants.TEST_USER_ID)).thenReturn(BigDecimal.ZERO);

        // When
        PortfolioService.PortfolioOverview overview = portfolioService.getPortfolioOverview(TestConstants.TEST_USER_ID);

        // Then
        assertNotNull(overview);
        assertEquals(testUser.getCurrentBalance(), overview.getCashBalance());
        assertEquals(BigDecimal.ZERO, overview.getTotalPortfolioValue());
        assertEquals(0, overview.getPositionCount());
        assertTrue(overview.getPositions().isEmpty());
    }

    @Test
    void testGetPortfolioSummary_Success() {
        // Given
        when(userDao.findById(TestConstants.TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(userHoldingDao.getTotalPortfolioValue(TestConstants.TEST_USER_ID)).thenReturn(TestConstants.PORTFOLIO_VALUE);
        when(userHoldingDao.getTotalInvestedByUser(TestConstants.TEST_USER_ID)).thenReturn(TestConstants.TOTAL_INVESTED);
        when(userHoldingDao.getUserHoldingCount(TestConstants.TEST_USER_ID)).thenReturn(2L);

        // When
        PortfolioService.PortfolioSummary summary = portfolioService.getPortfolioSummary(TestConstants.TEST_USER_ID);

        // Then
        assertNotNull(summary);
        assertEquals(testUser.getCurrentBalance(), summary.getCashBalance());
        assertEquals(TestConstants.PORTFOLIO_VALUE, summary.getPortfolioValue());
        assertEquals(TestConstants.TOTAL_INVESTED, summary.getTotalInvested());
        assertEquals(2L, summary.getHoldingCount());

        BigDecimal expectedTotalAccount = testUser.getCurrentBalance().add(TestConstants.PORTFOLIO_VALUE);
        assertEquals(expectedTotalAccount, summary.getTotalAccountValue());
    }

    // ===============================================
    // PORTFOLIO POSITIONS TESTS
    // ===============================================

    @Test
    void testGetPortfolioPositions_Success() {
        // Given
        List<UserHoldingDao.HoldingWithDetails> testHoldings = createBasicHoldingsWithDetails();
        when(userHoldingDao.findHoldingsWithDetailsByUserId(TestConstants.TEST_USER_ID)).thenReturn(testHoldings);

        // When
        List<PortfolioService.PortfolioPosition> positions = portfolioService.getPortfolioPositions(TestConstants.TEST_USER_ID);

        // Then
        assertNotNull(positions);
        assertEquals(1, positions.size());

        // Check first position
        PortfolioService.PortfolioPosition firstPosition = positions.get(0);
        assertNotNull(firstPosition);
        assertTrue(firstPosition.getCurrentValue().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void testGetPortfolioPosition_Found() {
        // Given
        when(cryptocurrencyDao.getIdBySymbol(TestConstants.BTC_SYMBOL)).thenReturn(TestConstants.TEST_CRYPTO_ID);
        when(userHoldingDao.findByUserAndCrypto(TestConstants.TEST_USER_ID, TestConstants.TEST_CRYPTO_ID)).thenReturn(Optional.of(testHolding));
        when(cryptocurrencyDao.findById(TestConstants.TEST_CRYPTO_ID)).thenReturn(Optional.of(testCrypto));

        // When
        Optional<PortfolioService.PortfolioPosition> position = portfolioService.getPortfolioPosition(TestConstants.TEST_USER_ID, TestConstants.BTC_SYMBOL);

        // Then
        assertTrue(position.isPresent());
        assertEquals(TestConstants.BTC_SYMBOL, position.get().getSymbol());
        assertEquals(testHolding.getQuantity(), position.get().getQuantity());
    }

    @Test
    void testGetPortfolioPosition_NotFound() {
        // Given
        when(cryptocurrencyDao.getIdBySymbol(TestConstants.BTC_SYMBOL)).thenReturn(TestConstants.TEST_CRYPTO_ID);
        when(userHoldingDao.findByUserAndCrypto(TestConstants.TEST_USER_ID, TestConstants.TEST_CRYPTO_ID)).thenReturn(Optional.empty());

        // When
        Optional<PortfolioService.PortfolioPosition> position = portfolioService.getPortfolioPosition(TestConstants.TEST_USER_ID, TestConstants.BTC_SYMBOL);

        // Then
        assertFalse(position.isPresent());
    }

    // ===============================================
    // PORTFOLIO ANALYTICS TESTS
    // ===============================================


    @Test
    void testGetPortfolioPerformance_Success() {
        // Given
        List<UserHoldingDao.HoldingWithDetails> testHoldings = createBasicHoldingsWithDetails();

        when(userDao.findById(TestConstants.TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(userHoldingDao.findHoldingsWithDetailsByUserId(TestConstants.TEST_USER_ID)).thenReturn(testHoldings);
        when(transactionDao.getTotalRealizedProfitLoss(TestConstants.TEST_USER_ID)).thenReturn(TestConstants.REALIZED_PNL);

        // When
        PortfolioService.PortfolioPerformance performance = portfolioService.getPortfolioPerformance(TestConstants.TEST_USER_ID);

        // Then
        assertNotNull(performance);
        assertTrue(performance.getWinners() >= 0);
        assertTrue(performance.getLosers() >= 0);
        assertTrue(performance.getDiversificationScore() > 0);
        assertEquals(TestConstants.REALIZED_PNL, performance.getTotalRealizedPnL());
    }

    // ===============================================
    // PORTFOLIO MANAGEMENT TESTS
    // ===============================================

    @Test
    void testUpdatePortfolioUnrealizedPnL_Success() {
        // Given - No specific mocking needed as this just calls DAO

        // When
        portfolioService.updatePortfolioUnrealizedPnL(TestConstants.TEST_USER_ID);

        // Then
        verify(userHoldingDao).updateAllUnrealizedProfitLoss(TestConstants.TEST_USER_ID);
    }

    @Test
    void testCleanupZeroHoldings_Success() {
        // Given
        when(userHoldingDao.deleteZeroQuantityHoldings()).thenReturn(3);

        // When
        int deletedCount = portfolioService.cleanupZeroHoldings();

        // Then
        assertEquals(3, deletedCount);
        verify(userHoldingDao).deleteZeroQuantityHoldings();
    }

    // ===============================================
    // PORTFOLIO UTILITIES TESTS
    // ===============================================

    @Test
    void testUserHasHoldings_True() {
        // Given
        when(userHoldingDao.getUserHoldingCount(TestConstants.TEST_USER_ID)).thenReturn(2L);

        // When
        boolean hasHoldings = portfolioService.userHasHoldings(TestConstants.TEST_USER_ID);

        // Then
        assertTrue(hasHoldings);
    }

    @Test
    void testUserHasHoldings_False() {
        // Given
        when(userHoldingDao.getUserHoldingCount(TestConstants.TEST_USER_ID)).thenReturn(0L);

        // When
        boolean hasHoldings = portfolioService.userHasHoldings(TestConstants.TEST_USER_ID);

        // Then
        assertFalse(hasHoldings);
    }

    @Test
    void testGetUserCryptoQuantity_Success() {
        // Given
        when(cryptocurrencyDao.getIdBySymbol(TestConstants.BTC_SYMBOL)).thenReturn(TestConstants.TEST_CRYPTO_ID);
        when(userHoldingDao.getUserCryptoQuantity(TestConstants.TEST_USER_ID, TestConstants.TEST_CRYPTO_ID)).thenReturn(TestConstants.TRADE_QUANTITY);

        // When
        BigDecimal quantity = portfolioService.getUserCryptoQuantity(TestConstants.TEST_USER_ID, TestConstants.BTC_SYMBOL);

        // Then
        assertEquals(TestConstants.TRADE_QUANTITY, quantity);
    }

    @Test
    void testCanSellQuantity_True() {
        // Given
        BigDecimal availableQuantity = TestConstants.TRADE_QUANTITY; // 0.02 BTC
        BigDecimal requestedQuantity = TestConstants.SMALL_QUANTITY; // 0.01 BTC (less than available)

        when(cryptocurrencyDao.getIdBySymbol(TestConstants.BTC_SYMBOL)).thenReturn(TestConstants.TEST_CRYPTO_ID);
        when(userHoldingDao.getUserCryptoQuantity(TestConstants.TEST_USER_ID, TestConstants.TEST_CRYPTO_ID)).thenReturn(availableQuantity);

        // When
        boolean canSell = portfolioService.canSellQuantity(TestConstants.TEST_USER_ID, TestConstants.BTC_SYMBOL, requestedQuantity);

        // Then
        assertTrue(canSell);
    }

    @Test
    void testCanSellQuantity_False() {
        // Given
        BigDecimal availableQuantity = TestConstants.SMALL_QUANTITY; // 0.01 BTC
        BigDecimal requestedQuantity = TestConstants.TRADE_QUANTITY; // 0.02 BTC (more than available)

        when(cryptocurrencyDao.getIdBySymbol(TestConstants.BTC_SYMBOL)).thenReturn(TestConstants.TEST_CRYPTO_ID);
        when(userHoldingDao.getUserCryptoQuantity(TestConstants.TEST_USER_ID, TestConstants.TEST_CRYPTO_ID)).thenReturn(availableQuantity);

        // When
        boolean canSell = portfolioService.canSellQuantity(TestConstants.TEST_USER_ID, TestConstants.BTC_SYMBOL, requestedQuantity);

        // Then
        assertFalse(canSell);
    }

    // ===============================================
    // VALIDATION TESTS
    // ===============================================

    @Test
    void testInvalidInputs_ThrowsExceptions() {
        // Test null user ID
        assertThrows(IllegalArgumentException.class, () ->
                portfolioService.getPortfolioOverview(null));

        // Test invalid user ID
        assertThrows(IllegalArgumentException.class, () ->
                portfolioService.getPortfolioOverview(-1L));

        // Test null crypto symbol
        assertThrows(IllegalArgumentException.class, () ->
                portfolioService.getUserCryptoQuantity(TestConstants.TEST_USER_ID, null));

        // Test empty crypto symbol
        assertThrows(IllegalArgumentException.class, () ->
                portfolioService.getUserCryptoQuantity(TestConstants.TEST_USER_ID, ""));
    }
}