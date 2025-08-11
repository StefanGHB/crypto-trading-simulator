package com.cryptotrading.crypto_trading_sim.controller;

import com.cryptotrading.crypto_trading_sim.dao.TransactionDao;
import com.cryptotrading.crypto_trading_sim.model.Transaction;
import com.cryptotrading.crypto_trading_sim.model.enums.TransactionType;
import com.cryptotrading.crypto_trading_sim.model.enums.TransactionStatus;
import com.cryptotrading.crypto_trading_sim.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST Controller for Transaction-related operations
 * Clean and simple endpoints that work directly with Service layer
 */
@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    // ===============================================
    // TRANSACTION RETRIEVAL ENDPOINTS
    // ===============================================

    /**
     * Get transaction by ID
     * GET /api/transactions/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getTransactionById(@PathVariable Long id) {
        try {
            Optional<Transaction> transaction = transactionService.getTransactionById(id);
            if (transaction.isPresent()) {
                return ResponseEntity.ok(transaction.get());
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Transaction not found"));
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get transaction: " + e.getMessage()));
        }
    }

    /**
     * Get all transactions for a user
     * GET /api/transactions/user/{userId}
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserTransactions(@PathVariable Long userId) {
        try {
            List<Transaction> transactions = transactionService.getUserTransactions(userId);
            return ResponseEntity.ok(transactions);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get transactions: " + e.getMessage()));
        }
    }

    /**
     * Get transactions with crypto details for a user
     * GET /api/transactions/user/{userId}/with-details
     */
    @GetMapping("/user/{userId}/with-details")
    public ResponseEntity<?> getUserTransactionsWithDetails(@PathVariable Long userId) {
        try {
            List<TransactionDao.TransactionWithDetails> transactions = transactionService.getUserTransactionsWithDetails(userId);
            return ResponseEntity.ok(transactions);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get detailed transactions: " + e.getMessage()));
        }
    }

    /**
     * Get recent transactions for a user (limited count)
     * GET /api/transactions/user/{userId}/recent?limit={limit}
     */
    @GetMapping("/user/{userId}/recent")
    public ResponseEntity<?> getRecentUserTransactions(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<Transaction> transactions = transactionService.getRecentUserTransactions(userId, limit);
            return ResponseEntity.ok(transactions);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get recent transactions: " + e.getMessage()));
        }
    }

    /**
     * Get transactions for a specific cryptocurrency
     * GET /api/transactions/user/{userId}/crypto/{symbol}
     */
    @GetMapping("/user/{userId}/crypto/{symbol}")
    public ResponseEntity<?> getUserTransactionsForCrypto(
            @PathVariable Long userId,
            @PathVariable String symbol) {
        try {
            List<Transaction> transactions = transactionService.getUserTransactionsForCrypto(userId, symbol);
            return ResponseEntity.ok(transactions);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get crypto transactions: " + e.getMessage()));
        }
    }

    /**
     * Get transactions by type (BUY or SELL)
     * GET /api/transactions/user/{userId}/type/{type}
     */
    @GetMapping("/user/{userId}/type/{type}")
    public ResponseEntity<?> getUserTransactionsByType(
            @PathVariable Long userId,
            @PathVariable TransactionType type) {
        try {
            List<Transaction> transactions = transactionService.getUserTransactionsByType(userId, type);
            return ResponseEntity.ok(transactions);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get transactions by type: " + e.getMessage()));
        }
    }

    /**
     * Get transactions within date range
     * GET /api/transactions/user/{userId}/date-range?startDate={startDate}&endDate={endDate}
     */
    @GetMapping("/user/{userId}/date-range")
    public ResponseEntity<?> getTransactionsByDateRange(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        try {
            List<Transaction> transactions = transactionService.getTransactionsByDateRange(userId, startDate, endDate);
            return ResponseEntity.ok(transactions);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get transactions by date range: " + e.getMessage()));
        }
    }

    // ===============================================
    // TRANSACTION ANALYTICS ENDPOINTS
    // ===============================================

    /**
     * Get comprehensive transaction analytics for user
     * GET /api/transactions/user/{userId}/analytics
     */
    @GetMapping("/user/{userId}/analytics")
    public ResponseEntity<?> getTransactionAnalytics(@PathVariable Long userId) {
        try {
            TransactionService.TransactionAnalytics analytics = transactionService.getTransactionAnalytics(userId);
            return ResponseEntity.ok(analytics);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get transaction analytics: " + e.getMessage()));
        }
    }

    /**
     * Get profit/loss breakdown by cryptocurrency
     * GET /api/transactions/user/{userId}/profit-loss-breakdown
     */
    @GetMapping("/user/{userId}/profit-loss-breakdown")
    public ResponseEntity<?> getProfitLossBreakdown(@PathVariable Long userId) {
        try {
            List<TransactionService.CryptoProfitLoss> breakdown = transactionService.getProfitLossBreakdown(userId);
            return ResponseEntity.ok(breakdown);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get P&L breakdown: " + e.getMessage()));
        }
    }

    /**
     * Get transaction summary for a specific period
     * GET /api/transactions/user/{userId}/period-summary?startDate={startDate}&endDate={endDate}
     */
    @GetMapping("/user/{userId}/period-summary")
    public ResponseEntity<?> getTransactionSummaryForPeriod(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        try {
            TransactionService.TransactionPeriodSummary summary = transactionService.getTransactionSummaryForPeriod(userId, startDate, endDate);
            return ResponseEntity.ok(summary);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get period summary: " + e.getMessage()));
        }
    }

    // ===============================================
    // TRANSACTION HISTORY EXPORT ENDPOINTS
    // ===============================================

    /**
     * Get transaction history formatted for export/display
     * GET /api/transactions/user/{userId}/export
     */
    @GetMapping("/user/{userId}/export")
    public ResponseEntity<?> getTransactionHistoryForExport(@PathVariable Long userId) {
        try {
            List<TransactionService.TransactionHistoryEntry> history = transactionService.getTransactionHistoryForExport(userId);
            return ResponseEntity.ok(history);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get transaction history: " + e.getMessage()));
        }
    }

    /**
     * Get transaction history for specific cryptocurrency
     * GET /api/transactions/user/{userId}/crypto/{symbol}/history
     */
    @GetMapping("/user/{userId}/crypto/{symbol}/history")
    public ResponseEntity<?> getTransactionHistoryForCrypto(
            @PathVariable Long userId,
            @PathVariable String symbol) {
        try {
            List<TransactionService.TransactionHistoryEntry> history = transactionService.getTransactionHistoryForCrypto(userId, symbol);
            return ResponseEntity.ok(history);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get crypto transaction history: " + e.getMessage()));
        }
    }

    // ===============================================
    // TRANSACTION MANAGEMENT ENDPOINTS
    // ===============================================

    /**
     * Update transaction status
     * PUT /api/transactions/{id}/status
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateTransactionStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        try {
            TransactionStatus status = TransactionStatus.valueOf(request.get("status"));
            boolean updated = transactionService.updateTransactionStatus(id, status);

            if (updated) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Status updated to " + status
                ));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Failed to update transaction status"));
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update status: " + e.getMessage()));
        }
    }

    /**
     * Get transaction count for user
     * GET /api/transactions/user/{userId}/count
     */
    @GetMapping("/user/{userId}/count")
    public ResponseEntity<?> getUserTransactionCount(@PathVariable Long userId) {
        try {
            long count = transactionService.getUserTransactionCount(userId);
            return ResponseEntity.ok(Map.of(
                    "userId", userId,
                    "transactionCount", count
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get transaction count: " + e.getMessage()));
        }
    }

    /**
     * Check if user has any transactions
     * GET /api/transactions/user/{userId}/has-transactions
     */
    @GetMapping("/user/{userId}/has-transactions")
    public ResponseEntity<?> userHasTransactions(@PathVariable Long userId) {
        try {
            boolean hasTransactions = transactionService.userHasTransactions(userId);
            return ResponseEntity.ok(Map.of(
                    "userId", userId,
                    "hasTransactions", hasTransactions
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to check transaction status: " + e.getMessage()));
        }
    }

    /**
     * Get transaction statistics summary
     * GET /api/transactions/user/{userId}/statistics
     */
    @GetMapping("/user/{userId}/statistics")
    public ResponseEntity<?> getTransactionStatistics(@PathVariable Long userId) {
        try {
            TransactionService.TransactionAnalytics analytics = transactionService.getTransactionAnalytics(userId);
            return ResponseEntity.ok(analytics);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get statistics: " + e.getMessage()));
        }
    }
}