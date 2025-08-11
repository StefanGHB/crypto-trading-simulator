package com.cryptotrading.crypto_trading_sim.model;

import com.cryptotrading.crypto_trading_sim.model.enums.TransactionStatus;
import com.cryptotrading.crypto_trading_sim.model.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Transaction entity representing a buy or sell transaction in the trading platform
 */
public class Transaction {
    private Long id;
    private Long userId;
    private Long cryptoId;
    private TransactionType transactionType;
    private BigDecimal quantity;
    private BigDecimal pricePerUnit;
    private BigDecimal totalAmount;
    private BigDecimal fees;
    private BigDecimal realizedProfitLoss; // Only for SELL transactions
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private TransactionStatus transactionStatus;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    // Default constructor
    public Transaction() {}

    // Constructor for new transaction
    public Transaction(Long userId, Long cryptoId, TransactionType transactionType,
                       BigDecimal quantity, BigDecimal pricePerUnit, BigDecimal balanceBefore) {
        this.userId = userId;
        this.cryptoId = cryptoId;
        this.transactionType = transactionType;
        this.quantity = quantity;
        this.pricePerUnit = pricePerUnit;
        this.totalAmount = quantity.multiply(pricePerUnit);
        this.fees = BigDecimal.ZERO;
        this.balanceBefore = balanceBefore;
        this.transactionStatus = TransactionStatus.COMPLETED;
        this.createdAt = LocalDateTime.now();
        this.completedAt = LocalDateTime.now();

        // Calculate balance after
        if (transactionType == TransactionType.BUY) {
            this.balanceAfter = balanceBefore.subtract(totalAmount);
        } else {
            this.balanceAfter = balanceBefore.add(totalAmount);
        }
    }

    // Full constructor
    public Transaction(Long id, Long userId, Long cryptoId, TransactionType transactionType,
                       BigDecimal quantity, BigDecimal pricePerUnit, BigDecimal totalAmount,
                       BigDecimal fees, BigDecimal realizedProfitLoss, BigDecimal balanceBefore,
                       BigDecimal balanceAfter, TransactionStatus transactionStatus,
                       LocalDateTime createdAt, LocalDateTime completedAt) {
        this.id = id;
        this.userId = userId;
        this.cryptoId = cryptoId;
        this.transactionType = transactionType;
        this.quantity = quantity;
        this.pricePerUnit = pricePerUnit;
        this.totalAmount = totalAmount;
        this.fees = fees;
        this.realizedProfitLoss = realizedProfitLoss;
        this.balanceBefore = balanceBefore;
        this.balanceAfter = balanceAfter;
        this.transactionStatus = transactionStatus;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getCryptoId() { return cryptoId; }
    public void setCryptoId(Long cryptoId) { this.cryptoId = cryptoId; }

    public TransactionType getTransactionType() { return transactionType; }
    public void setTransactionType(TransactionType transactionType) { this.transactionType = transactionType; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public BigDecimal getPricePerUnit() { return pricePerUnit; }
    public void setPricePerUnit(BigDecimal pricePerUnit) { this.pricePerUnit = pricePerUnit; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public BigDecimal getFees() { return fees; }
    public void setFees(BigDecimal fees) { this.fees = fees; }

    public BigDecimal getRealizedProfitLoss() { return realizedProfitLoss; }
    public void setRealizedProfitLoss(BigDecimal realizedProfitLoss) { this.realizedProfitLoss = realizedProfitLoss; }

    public BigDecimal getBalanceBefore() { return balanceBefore; }
    public void setBalanceBefore(BigDecimal balanceBefore) { this.balanceBefore = balanceBefore; }

    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(BigDecimal balanceAfter) { this.balanceAfter = balanceAfter; }

    public TransactionStatus getTransactionStatus() { return transactionStatus; }
    public void setTransactionStatus(TransactionStatus transactionStatus) { this.transactionStatus = transactionStatus; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    // Business methods
    public boolean isBuy() {
        return transactionType == TransactionType.BUY;
    }

    public boolean isSell() {
        return transactionType == TransactionType.SELL;
    }

    public boolean isCompleted() {
        return transactionStatus == TransactionStatus.COMPLETED;
    }

    public boolean isPending() {
        return transactionStatus == TransactionStatus.PENDING;
    }

    public boolean isFailed() {
        return transactionStatus == TransactionStatus.FAILED;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + id +
                ", userId=" + userId +
                ", cryptoId=" + cryptoId +
                ", transactionType=" + transactionType +
                ", quantity=" + quantity +
                ", pricePerUnit=" + pricePerUnit +
                ", totalAmount=" + totalAmount +
                ", createdAt=" + createdAt +
                '}';
    }
}