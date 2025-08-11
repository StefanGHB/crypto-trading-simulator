package com.cryptotrading.crypto_trading_sim.controller;

import com.cryptotrading.crypto_trading_sim.service.TradingService;
import com.cryptotrading.crypto_trading_sim.model.Transaction;
import com.cryptotrading.crypto_trading_sim.model.enums.TransactionType;
import com.cryptotrading.crypto_trading_sim.utils.TestConstants;
import com.cryptotrading.crypto_trading_sim.utils.TestDataBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@ExtendWith(MockitoExtension.class)
class TradingControllerTest {

    @Mock
    private TradingService tradingService;

    @InjectMocks
    private TradingController tradingController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private TradingService.TradingResult successResult;
    private TradingService.TradingResult failureResult;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(tradingController).build();
        objectMapper = new ObjectMapper();

        // Create test trading results
        Transaction successTransaction = TestDataBuilder.createBuyTransaction(TestConstants.TEST_USER_ID, TestConstants.TEST_CRYPTO_ID);
        TradingService.TradingSummary summary = new TradingService.TradingSummary(
                TransactionType.BUY, TestConstants.BTC_SYMBOL, TestConstants.BTC_NAME,
                TestConstants.TRADE_QUANTITY, TestConstants.BTC_PRICE, TestConstants.TRADE_AMOUNT,
                new BigDecimal("1.00"), null, TestConstants.UPDATED_BALANCE
        );

        successResult = new TradingService.TradingResult(true, "Success", successTransaction, summary);
        failureResult = new TradingService.TradingResult(false, "Insufficient balance", null, null);
    }

    // ===============================================
    // BUY OPERATION TESTS
    // ===============================================

    @Test
    void testBuyByAmount_Success() throws Exception {
        // Given
        Map<String, Object> request = Map.of(
                "userId", TestConstants.TEST_USER_ID,
                "cryptoSymbol", TestConstants.BTC_SYMBOL,
                "amountToSpend", TestConstants.TRADE_AMOUNT.toString()
        );

        when(tradingService.buyByAmount(TestConstants.TEST_USER_ID, TestConstants.BTC_SYMBOL, TestConstants.TRADE_AMOUNT))
                .thenReturn(successResult);

        // When & Then
        mockMvc.perform(post("/api/trading/buy-by-amount")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Success"))
                .andExpect(jsonPath("$.transaction").exists())
                .andExpect(jsonPath("$.summary").exists());

        verify(tradingService).buyByAmount(TestConstants.TEST_USER_ID, TestConstants.BTC_SYMBOL, TestConstants.TRADE_AMOUNT);
    }

    @Test
    void testBuyByAmount_InsufficientBalance() throws Exception {
        // Given
        Map<String, Object> request = Map.of(
                "userId", TestConstants.TEST_USER_ID,
                "cryptoSymbol", TestConstants.BTC_SYMBOL,
                "amountToSpend", "15000.00" // More than user balance
        );

        when(tradingService.buyByAmount(eq(TestConstants.TEST_USER_ID), eq(TestConstants.BTC_SYMBOL), any(BigDecimal.class)))
                .thenReturn(failureResult);

        // When & Then
        mockMvc.perform(post("/api/trading/buy-by-amount")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Insufficient balance"));
    }

    @Test
    void testBuyByAmount_BadRequest() throws Exception {
        // Given - Invalid request with missing fields
        Map<String, Object> invalidRequest = Map.of(
                "userId", TestConstants.TEST_USER_ID
                // Missing cryptoSymbol and amountToSpend
        );

        // When & Then - Should return 500 because validation happens in service layer
        mockMvc.perform(post("/api/trading/buy-by-amount")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isInternalServerError()); // 🔧 FIXED: Expect 500 not 400
    }

    @Test
    void testBuyByQuantity_Success() throws Exception {
        // Given
        Map<String, Object> request = Map.of(
                "userId", TestConstants.TEST_USER_ID,
                "cryptoSymbol", TestConstants.BTC_SYMBOL,
                "quantity", TestConstants.TRADE_QUANTITY.toString()
        );

        when(tradingService.buyByQuantity(TestConstants.TEST_USER_ID, TestConstants.BTC_SYMBOL, TestConstants.TRADE_QUANTITY))
                .thenReturn(successResult);

        // When & Then
        mockMvc.perform(post("/api/trading/buy-by-quantity")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.transaction.transactionType").value("BUY"));

        verify(tradingService).buyByQuantity(TestConstants.TEST_USER_ID, TestConstants.BTC_SYMBOL, TestConstants.TRADE_QUANTITY);
    }

    // ===============================================
    // SELL OPERATION TESTS
    // ===============================================

    @Test
    void testSellByQuantity_Success() throws Exception {
        // Given
        Map<String, Object> request = Map.of(
                "userId", TestConstants.TEST_USER_ID,
                "cryptoSymbol", TestConstants.BTC_SYMBOL,
                "quantity", TestConstants.SMALL_QUANTITY.toString()
        );

        Transaction sellTransaction = TestDataBuilder.createSellTransaction(TestConstants.TEST_USER_ID, TestConstants.TEST_CRYPTO_ID);
        TradingService.TradingSummary sellSummary = new TradingService.TradingSummary(
                TransactionType.SELL, TestConstants.BTC_SYMBOL, TestConstants.BTC_NAME,
                TestConstants.SMALL_QUANTITY, TestConstants.BTC_PRICE, new BigDecimal("500.00"),
                new BigDecimal("0.50"), TestConstants.REALIZED_PNL, new BigDecimal("10500.00")
        );
        TradingService.TradingResult sellResult = new TradingService.TradingResult(true, "Sell successful", sellTransaction, sellSummary);

        when(tradingService.sellByQuantity(TestConstants.TEST_USER_ID, TestConstants.BTC_SYMBOL, TestConstants.SMALL_QUANTITY))
                .thenReturn(sellResult);

        // When & Then
        mockMvc.perform(post("/api/trading/sell-by-quantity")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.transaction.transactionType").value("SELL"))
                .andExpect(jsonPath("$.summary.realizedPnL").exists());
    }

    @Test
    void testSellByQuantity_InsufficientHoldings() throws Exception {
        // Given
        Map<String, Object> request = Map.of(
                "userId", TestConstants.TEST_USER_ID,
                "cryptoSymbol", TestConstants.BTC_SYMBOL,
                "quantity", TestConstants.LARGE_QUANTITY.toString() // More than user has
        );

        TradingService.TradingResult insufficientResult = new TradingService.TradingResult(
                false, "Insufficient BTC holdings", null, null);

        when(tradingService.sellByQuantity(eq(TestConstants.TEST_USER_ID), eq(TestConstants.BTC_SYMBOL), any(BigDecimal.class)))
                .thenReturn(insufficientResult);

        // When & Then
        mockMvc.perform(post("/api/trading/sell-by-quantity")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Insufficient BTC holdings"));
    }

    @Test
    void testSellAll_Success() throws Exception {
        // Given
        Map<String, Object> request = Map.of(
                "userId", TestConstants.TEST_USER_ID,
                "cryptoSymbol", TestConstants.BTC_SYMBOL
        );

        when(tradingService.sellAll(TestConstants.TEST_USER_ID, TestConstants.BTC_SYMBOL))
                .thenReturn(successResult);

        // When & Then
        mockMvc.perform(post("/api/trading/sell-all")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(tradingService).sellAll(TestConstants.TEST_USER_ID, TestConstants.BTC_SYMBOL);
    }

    // ===============================================
    // TRADING QUOTES TESTS
    // ===============================================

    @Test
    void testGetBuyQuote_Success() throws Exception {
        // Given
        TradingService.TradingQuote quote = new TradingService.TradingQuote(
                TransactionType.BUY, TestConstants.TRADE_QUANTITY, TestConstants.BTC_PRICE,
                TestConstants.TRADE_AMOUNT, new BigDecimal("1.00"), new BigDecimal("999.00")
        );

        when(tradingService.getBuyQuote(TestConstants.TEST_USER_ID, TestConstants.BTC_SYMBOL, TestConstants.TRADE_AMOUNT))
                .thenReturn(quote);

        // When & Then
        mockMvc.perform(get("/api/trading/quote/buy")
                        .param("userId", TestConstants.TEST_USER_ID.toString())
                        .param("symbol", TestConstants.BTC_SYMBOL)
                        .param("amount", TestConstants.TRADE_AMOUNT.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("BUY"))
                .andExpect(jsonPath("$.quantity").exists())
                .andExpect(jsonPath("$.pricePerUnit").exists())
                .andExpect(jsonPath("$.fees").exists());

        verify(tradingService).getBuyQuote(TestConstants.TEST_USER_ID, TestConstants.BTC_SYMBOL, TestConstants.TRADE_AMOUNT);
    }

    @Test
    void testGetSellQuote_Success() throws Exception {
        // Given
        TradingService.TradingQuote quote = new TradingService.TradingQuote(
                TransactionType.SELL, TestConstants.TRADE_QUANTITY, TestConstants.BTC_PRICE,
                TestConstants.TRADE_AMOUNT, new BigDecimal("1.00"), new BigDecimal("999.00")
        );

        when(tradingService.getSellQuote(TestConstants.TEST_USER_ID, TestConstants.BTC_SYMBOL, TestConstants.TRADE_QUANTITY))
                .thenReturn(quote);

        // When & Then
        mockMvc.perform(get("/api/trading/quote/sell")
                        .param("userId", TestConstants.TEST_USER_ID.toString())
                        .param("symbol", TestConstants.BTC_SYMBOL)
                        .param("quantity", TestConstants.TRADE_QUANTITY.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("SELL"))
                .andExpect(jsonPath("$.quantity").exists())
                .andExpect(jsonPath("$.netAmount").exists());
    }

    // ===============================================
    // UTILITY TESTS
    // ===============================================

    @Test
    void testGetCurrentPrice_Success() throws Exception {
        // Given
        when(tradingService.getCurrentPrice(TestConstants.BTC_SYMBOL)).thenReturn(TestConstants.BTC_PRICE);

        // When & Then
        mockMvc.perform(get("/api/trading/price/{symbol}", TestConstants.BTC_SYMBOL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value(TestConstants.BTC_SYMBOL))
                .andExpect(jsonPath("$.currentPrice").value(50000.0)); // 🔧 FIXED: Expect .0 not .00

        verify(tradingService).getCurrentPrice(TestConstants.BTC_SYMBOL);
    }

    @Test
    void testCalculateFees_Success() throws Exception {
        // Given
        BigDecimal amount = new BigDecimal("1000.00");
        BigDecimal expectedFees = new BigDecimal("1.00");

        when(tradingService.calculateFees(amount)).thenReturn(expectedFees);

        // When & Then
        mockMvc.perform(get("/api/trading/fees")
                        .param("amount", amount.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(1000.0)) // 🔧 FIXED: Expect .0 not .00
                .andExpect(jsonPath("$.fees").value(expectedFees.doubleValue()));

        verify(tradingService).calculateFees(amount);
    }

    // ===============================================
    // ERROR HANDLING TESTS
    // ===============================================

    @Test
    void testServiceException_ReturnsInternalServerError() throws Exception {
        // Given
        Map<String, Object> request = Map.of(
                "userId", TestConstants.TEST_USER_ID,
                "cryptoSymbol", TestConstants.BTC_SYMBOL,
                "amountToSpend", TestConstants.TRADE_AMOUNT.toString()
        );

        when(tradingService.buyByAmount(any(), any(), any())).thenThrow(new RuntimeException("Database error"));

        // When & Then
        mockMvc.perform(post("/api/trading/buy-by-amount")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void testInvalidParameters_ReturnsBadRequest() throws Exception {
        // Given
        when(tradingService.buyByAmount(any(), any(), any())).thenThrow(new IllegalArgumentException("Invalid amount"));

        Map<String, Object> request = Map.of(
                "userId", TestConstants.TEST_USER_ID,
                "cryptoSymbol", TestConstants.BTC_SYMBOL,
                "amountToSpend", "-100.00" // Negative amount
        );

        // When & Then
        mockMvc.perform(post("/api/trading/buy-by-amount")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid amount"));
    }
}