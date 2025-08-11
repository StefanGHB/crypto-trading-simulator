package com.cryptotrading.crypto_trading_sim.controller;

import com.cryptotrading.crypto_trading_sim.service.PortfolioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST Controller for Portfolio-related operations
 * Clean and simple endpoints that work directly with Service layer
 */
@RestController
@RequestMapping("/api/portfolio")
public class PortfolioController {

    @Autowired
    private PortfolioService portfolioService;

    // ===============================================
    // PORTFOLIO OVERVIEW ENDPOINTS
    // ===============================================

    /**
     * Get complete portfolio overview for user
     * GET /api/portfolio/user/{userId}/overview
     */
    @GetMapping("/user/{userId}/overview")
    public ResponseEntity<?> getPortfolioOverview(@PathVariable Long userId) {
        try {
            PortfolioService.PortfolioOverview overview = portfolioService.getPortfolioOverview(userId);
            return ResponseEntity.ok(overview);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get portfolio overview: " + e.getMessage()));
        }
    }

    /**
     * Get simplified portfolio summary
     * GET /api/portfolio/user/{userId}/summary
     */
    @GetMapping("/user/{userId}/summary")
    public ResponseEntity<?> getPortfolioSummary(@PathVariable Long userId) {
        try {
            PortfolioService.PortfolioSummary summary = portfolioService.getPortfolioSummary(userId);
            return ResponseEntity.ok(summary);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get portfolio summary: " + e.getMessage()));
        }
    }

    // ===============================================
    // PORTFOLIO POSITIONS ENDPOINTS
    // ===============================================

    /**
     * Get all portfolio positions for user
     * GET /api/portfolio/user/{userId}/positions
     */
    @GetMapping("/user/{userId}/positions")
    public ResponseEntity<?> getPortfolioPositions(@PathVariable Long userId) {
        try {
            List<PortfolioService.PortfolioPosition> positions = portfolioService.getPortfolioPositions(userId);
            return ResponseEntity.ok(positions);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get portfolio positions: " + e.getMessage()));
        }
    }

    /**
     * Get specific portfolio position for cryptocurrency
     * GET /api/portfolio/user/{userId}/position/{symbol}
     */
    @GetMapping("/user/{userId}/position/{symbol}")
    public ResponseEntity<?> getPortfolioPosition(
            @PathVariable Long userId,
            @PathVariable String symbol) {
        try {
            Optional<PortfolioService.PortfolioPosition> position = portfolioService.getPortfolioPosition(userId, symbol);
            if (position.isPresent()) {
                return ResponseEntity.ok(position.get());
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "No position found for " + symbol));
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get portfolio position: " + e.getMessage()));
        }
    }

    // ===============================================
    // PORTFOLIO ANALYTICS ENDPOINTS
    // ===============================================

    /**
     * Get portfolio allocation breakdown (by percentage)
     * GET /api/portfolio/user/{userId}/allocation
     */
    @GetMapping("/user/{userId}/allocation")
    public ResponseEntity<?> getPortfolioAllocation(@PathVariable Long userId) {
        try {
            List<PortfolioService.PortfolioAllocation> allocation = portfolioService.getPortfolioAllocation(userId);
            return ResponseEntity.ok(allocation);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get portfolio allocation: " + e.getMessage()));
        }
    }

    /**
     * Get portfolio performance metrics
     * GET /api/portfolio/user/{userId}/performance
     */
    @GetMapping("/user/{userId}/performance")
    public ResponseEntity<?> getPortfolioPerformance(@PathVariable Long userId) {
        try {
            PortfolioService.PortfolioPerformance performance = portfolioService.getPortfolioPerformance(userId);
            return ResponseEntity.ok(performance);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get portfolio performance: " + e.getMessage()));
        }
    }

    // ===============================================
    // PORTFOLIO MANAGEMENT ENDPOINTS
    // ===============================================

    /**
     * Update all unrealized P&L for user's portfolio
     * POST /api/portfolio/user/{userId}/update-pnl
     */
    @PostMapping("/user/{userId}/update-pnl")
    public ResponseEntity<?> updatePortfolioUnrealizedPnL(@PathVariable Long userId) {
        try {
            portfolioService.updatePortfolioUnrealizedPnL(userId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Portfolio P&L updated successfully"
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update portfolio P&L: " + e.getMessage()));
        }
    }

    /**
     * Clean up zero-quantity holdings
     * POST /api/portfolio/cleanup-zero-holdings
     */
    @PostMapping("/cleanup-zero-holdings")
    public ResponseEntity<?> cleanupZeroHoldings() {
        try {
            int deletedCount = portfolioService.cleanupZeroHoldings();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "deletedHoldings", deletedCount,
                    "message", "Zero holdings cleanup completed"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to cleanup holdings: " + e.getMessage()));
        }
    }

    // ===============================================
    // PORTFOLIO UTILITIES ENDPOINTS
    // ===============================================

    /**
     * Check if user has any holdings
     * GET /api/portfolio/user/{userId}/has-holdings
     */
    @GetMapping("/user/{userId}/has-holdings")
    public ResponseEntity<?> userHasHoldings(@PathVariable Long userId) {
        try {
            boolean hasHoldings = portfolioService.userHasHoldings(userId);
            return ResponseEntity.ok(Map.of(
                    "userId", userId,
                    "hasHoldings", hasHoldings
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to check holdings status: " + e.getMessage()));
        }
    }

    /**
     * Get user's quantity for specific cryptocurrency
     * GET /api/portfolio/user/{userId}/quantity/{symbol}
     */
    @GetMapping("/user/{userId}/quantity/{symbol}")
    public ResponseEntity<?> getUserCryptoQuantity(
            @PathVariable Long userId,
            @PathVariable String symbol) {
        try {
            BigDecimal quantity = portfolioService.getUserCryptoQuantity(userId, symbol);
            return ResponseEntity.ok(Map.of(
                    "userId", userId,
                    "symbol", symbol,
                    "quantity", quantity
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get crypto quantity: " + e.getMessage()));
        }
    }

    /**
     * Check if user can sell specific quantity of cryptocurrency
     * GET /api/portfolio/user/{userId}/can-sell?symbol={symbol}&quantity={quantity}
     */
    @GetMapping("/user/{userId}/can-sell")
    public ResponseEntity<?> canSellQuantity(
            @PathVariable Long userId,
            @RequestParam String symbol,
            @RequestParam BigDecimal quantity) {
        try {
            boolean canSell = portfolioService.canSellQuantity(userId, symbol, quantity);
            BigDecimal availableQuantity = portfolioService.getUserCryptoQuantity(userId, symbol);

            return ResponseEntity.ok(Map.of(
                    "userId", userId,
                    "symbol", symbol,
                    "requestedQuantity", quantity,
                    "availableQuantity", availableQuantity,
                    "canSell", canSell
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to check sellability: " + e.getMessage()));
        }
    }

    // ===============================================
    // PORTFOLIO DASHBOARD ENDPOINTS
    // ===============================================

    /**
     * Get comprehensive portfolio dashboard data
     * GET /api/portfolio/user/{userId}/dashboard
     */
    @GetMapping("/user/{userId}/dashboard")
    public ResponseEntity<?> getPortfolioDashboard(@PathVariable Long userId) {
        try {
            // Get all necessary data for dashboard
            PortfolioService.PortfolioOverview overview = portfolioService.getPortfolioOverview(userId);
            List<PortfolioService.PortfolioAllocation> allocation = portfolioService.getPortfolioAllocation(userId);
            PortfolioService.PortfolioPerformance performance = portfolioService.getPortfolioPerformance(userId);

            // Return combined dashboard data
            return ResponseEntity.ok(Map.of(
                    "overview", overview,
                    "allocation", allocation,
                    "performance", performance
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get portfolio dashboard: " + e.getMessage()));
        }
    }

    /**
     * Get portfolio value history (simplified for now - returns current value)
     * GET /api/portfolio/user/{userId}/value-history
     */
    @GetMapping("/user/{userId}/value-history")
    public ResponseEntity<?> getPortfolioValueHistory(@PathVariable Long userId) {
        try {
            PortfolioService.PortfolioSummary summary = portfolioService.getPortfolioSummary(userId);

            // For now, return current value - in future this could track historical values
            return ResponseEntity.ok(Map.of(
                    "userId", userId,
                    "currentValue", summary.getTotalAccountValue(),
                    "timestamp", java.time.LocalDateTime.now()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get value history: " + e.getMessage()));
        }
    }
}