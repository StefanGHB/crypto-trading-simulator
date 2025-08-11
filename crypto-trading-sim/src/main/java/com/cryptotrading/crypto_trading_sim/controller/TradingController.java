package com.cryptotrading.crypto_trading_sim.controller;

import com.cryptotrading.crypto_trading_sim.service.TradingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * REST Controller for Trading-related operations
 * Clean and simple endpoints that work directly with Service layer
 */
@RestController
@RequestMapping("/api/trading")
public class TradingController {

    @Autowired
    private TradingService tradingService;

    // ===============================================
    // BUY ORDER ENDPOINTS
    // ===============================================

    /**
     * Execute buy order by amount (specify USD amount to spend)
     * POST /api/trading/buy-by-amount
     */
    @PostMapping("/buy-by-amount")
    public ResponseEntity<?> buyByAmount(@RequestBody Map<String, Object> request) {
        try {
            Long userId = Long.valueOf(request.get("userId").toString());
            String cryptoSymbol = (String) request.get("cryptoSymbol");
            BigDecimal amountToSpend = new BigDecimal(request.get("amountToSpend").toString());

            TradingService.TradingResult result = tradingService.buyByAmount(userId, cryptoSymbol, amountToSpend);

            if (result.isSuccess()) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Trading failed: " + e.getMessage()));
        }
    }

    /**
     * Execute buy order by quantity (specify crypto quantity to buy)
     * POST /api/trading/buy-by-quantity
     */
    @PostMapping("/buy-by-quantity")
    public ResponseEntity<?> buyByQuantity(@RequestBody Map<String, Object> request) {
        try {
            Long userId = Long.valueOf(request.get("userId").toString());
            String cryptoSymbol = (String) request.get("cryptoSymbol");
            BigDecimal quantity = new BigDecimal(request.get("quantity").toString());

            TradingService.TradingResult result = tradingService.buyByQuantity(userId, cryptoSymbol, quantity);

            if (result.isSuccess()) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Trading failed: " + e.getMessage()));
        }
    }

    // ===============================================
    // SELL ORDER ENDPOINTS
    // ===============================================

    /**
     * Execute sell order by amount (specify USD amount to receive)
     * POST /api/trading/sell-by-amount
     */
    @PostMapping("/sell-by-amount")
    public ResponseEntity<?> sellByAmount(@RequestBody Map<String, Object> request) {
        try {
            Long userId = Long.valueOf(request.get("userId").toString());
            String cryptoSymbol = (String) request.get("cryptoSymbol");
            BigDecimal amountToReceive = new BigDecimal(request.get("amountToReceive").toString());

            TradingService.TradingResult result = tradingService.sellByAmount(userId, cryptoSymbol, amountToReceive);

            if (result.isSuccess()) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Trading failed: " + e.getMessage()));
        }
    }

    /**
     * Execute sell order by quantity (specify crypto quantity to sell)
     * POST /api/trading/sell-by-quantity
     */
    @PostMapping("/sell-by-quantity")
    public ResponseEntity<?> sellByQuantity(@RequestBody Map<String, Object> request) {
        try {
            Long userId = Long.valueOf(request.get("userId").toString());
            String cryptoSymbol = (String) request.get("cryptoSymbol");
            BigDecimal quantity = new BigDecimal(request.get("quantity").toString());

            TradingService.TradingResult result = tradingService.sellByQuantity(userId, cryptoSymbol, quantity);

            if (result.isSuccess()) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Trading failed: " + e.getMessage()));
        }
    }

    /**
     * Sell all holdings of a specific cryptocurrency
     * POST /api/trading/sell-all
     */
    @PostMapping("/sell-all")
    public ResponseEntity<?> sellAll(@RequestBody Map<String, Object> request) {
        try {
            Long userId = Long.valueOf(request.get("userId").toString());
            String cryptoSymbol = (String) request.get("cryptoSymbol");

            TradingService.TradingResult result = tradingService.sellAll(userId, cryptoSymbol);

            if (result.isSuccess()) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Trading failed: " + e.getMessage()));
        }
    }

    // ===============================================
    // TRADING QUOTES ENDPOINTS
    // ===============================================

    /**
     * Get buy quote (preview buy order without executing)
     * GET /api/trading/quote/buy?userId={userId}&symbol={symbol}&amount={amount}
     */
    @GetMapping("/quote/buy")
    public ResponseEntity<?> getBuyQuote(
            @RequestParam Long userId,
            @RequestParam String symbol,
            @RequestParam BigDecimal amount) {
        try {
            TradingService.TradingQuote quote = tradingService.getBuyQuote(userId, symbol, amount);
            return ResponseEntity.ok(quote);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to generate buy quote: " + e.getMessage()));
        }
    }

    /**
     * Get sell quote (preview sell order without executing)
     * GET /api/trading/quote/sell?userId={userId}&symbol={symbol}&quantity={quantity}
     */
    @GetMapping("/quote/sell")
    public ResponseEntity<?> getSellQuote(
            @RequestParam Long userId,
            @RequestParam String symbol,
            @RequestParam BigDecimal quantity) {
        try {
            TradingService.TradingQuote quote = tradingService.getSellQuote(userId, symbol, quantity);
            return ResponseEntity.ok(quote);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to generate sell quote: " + e.getMessage()));
        }
    }

    // ===============================================
    // TRADING UTILITIES ENDPOINTS
    // ===============================================

    /**
     * Get current market price for trading
     * GET /api/trading/price/{symbol}
     */
    @GetMapping("/price/{symbol}")
    public ResponseEntity<?> getCurrentPrice(@PathVariable String symbol) {
        try {
            BigDecimal currentPrice = tradingService.getCurrentPrice(symbol);
            return ResponseEntity.ok(Map.of(
                    "symbol", symbol,
                    "currentPrice", currentPrice
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get trading price: " + e.getMessage()));
        }
    }

    /**
     * Calculate trading fees for a given amount
     * GET /api/trading/fees?amount={amount}
     */
    @GetMapping("/fees")
    public ResponseEntity<?> calculateFees(@RequestParam BigDecimal amount) {
        try {
            BigDecimal fees = tradingService.calculateFees(amount);
            return ResponseEntity.ok(Map.of(
                    "amount", amount,
                    "fees", fees
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to calculate fees: " + e.getMessage()));
        }
    }
}