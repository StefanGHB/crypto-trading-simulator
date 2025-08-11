package com.cryptotrading.crypto_trading_sim.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * UserHolding entity representing a user's cryptocurrency holding/position
 * 🔧 PERFECT FIX: Proper fee inclusion in cost basis calculations with perfect logic
 */
public class UserHolding {
    private Long id;
    private Long userId;
    private Long cryptoId;
    private BigDecimal quantity;
    private BigDecimal averageBuyPrice;
    private BigDecimal totalInvested;
    private BigDecimal unrealizedProfitLoss;
    private LocalDateTime firstPurchaseAt;
    private LocalDateTime lastUpdated;

    // Default constructor
    public UserHolding() {}

    // Constructor for new holding
    public UserHolding(Long userId, Long cryptoId, BigDecimal quantity, BigDecimal averageBuyPrice) {
        this.userId = userId;
        this.cryptoId = cryptoId;
        this.quantity = quantity;
        this.averageBuyPrice = averageBuyPrice;
        this.totalInvested = quantity.multiply(averageBuyPrice);
        this.unrealizedProfitLoss = BigDecimal.ZERO;
        this.firstPurchaseAt = LocalDateTime.now();
        this.lastUpdated = LocalDateTime.now();
    }

    // Full constructor
    public UserHolding(Long id, Long userId, Long cryptoId, BigDecimal quantity,
                       BigDecimal averageBuyPrice, BigDecimal totalInvested,
                       BigDecimal unrealizedProfitLoss, LocalDateTime firstPurchaseAt,
                       LocalDateTime lastUpdated) {
        this.id = id;
        this.userId = userId;
        this.cryptoId = cryptoId;
        this.quantity = quantity;
        this.averageBuyPrice = averageBuyPrice;
        this.totalInvested = totalInvested;
        this.unrealizedProfitLoss = unrealizedProfitLoss;
        this.firstPurchaseAt = firstPurchaseAt;
        this.lastUpdated = lastUpdated;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getCryptoId() { return cryptoId; }
    public void setCryptoId(Long cryptoId) { this.cryptoId = cryptoId; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
        this.lastUpdated = LocalDateTime.now();
    }

    public BigDecimal getAverageBuyPrice() { return averageBuyPrice; }
    public void setAverageBuyPrice(BigDecimal averageBuyPrice) { this.averageBuyPrice = averageBuyPrice; }

    public BigDecimal getTotalInvested() { return totalInvested; }
    public void setTotalInvested(BigDecimal totalInvested) { this.totalInvested = totalInvested; }

    public BigDecimal getUnrealizedProfitLoss() { return unrealizedProfitLoss; }
    public void setUnrealizedProfitLoss(BigDecimal unrealizedProfitLoss) {
        this.unrealizedProfitLoss = unrealizedProfitLoss;
    }

    public LocalDateTime getFirstPurchaseAt() { return firstPurchaseAt; }
    public void setFirstPurchaseAt(LocalDateTime firstPurchaseAt) { this.firstPurchaseAt = firstPurchaseAt; }

    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }

    // ===============================================
    // 🔧 PERFECT BUSINESS METHODS
    // ===============================================

    /**
     * 🔧 ORIGINAL METHOD: Add purchase without considering fees (for backward compatibility)
     * This method is kept for existing functionality that might depend on it
     */
    public void addPurchase(BigDecimal newQuantity, BigDecimal newPrice) {
        BigDecimal totalCost = this.quantity.multiply(this.averageBuyPrice);
        BigDecimal newCost = newQuantity.multiply(newPrice);
        BigDecimal totalNewQuantity = this.quantity.add(newQuantity);

        this.averageBuyPrice = totalCost.add(newCost).divide(totalNewQuantity, 8, RoundingMode.HALF_UP);
        this.quantity = totalNewQuantity;
        this.totalInvested = this.totalInvested.add(newCost);
        this.lastUpdated = LocalDateTime.now();
    }

    /**
     * 🔧 PERFECT METHOD: Add purchase with fees properly included
     * This ensures that the true cost basis includes all trading fees
     */
    public void addPurchaseWithFees(BigDecimal newQuantity, BigDecimal newPrice, BigDecimal fees, BigDecimal totalAmountPaid) {
        // Calculate existing total cost (already includes fees from previous purchases)
        BigDecimal existingTotalCost = this.totalInvested;

        // Total new quantity
        BigDecimal totalNewQuantity = this.quantity.add(newQuantity);

        // New total invested including fees
        BigDecimal newTotalInvested = existingTotalCost.add(totalAmountPaid);

        // Calculate new average buy price that includes fees
        // This represents the true cost per unit including all fees
        BigDecimal newAverageBuyPrice = newTotalInvested.divide(totalNewQuantity, 8, RoundingMode.HALF_UP);

        // 🔧 PERFECT DEBUG LOGGING
        System.out.println("=== PERFECT PURCHASE CALCULATION ===");
        System.out.println("Existing quantity: " + this.quantity);
        System.out.println("Existing total invested: " + existingTotalCost);
        System.out.println("New quantity: " + newQuantity);
        System.out.println("New price: " + newPrice);
        System.out.println("Fees: " + fees);
        System.out.println("Total amount paid: " + totalAmountPaid);
        System.out.println("Total new quantity: " + totalNewQuantity);
        System.out.println("New total invested: " + newTotalInvested);
        System.out.println("New average buy price: " + newAverageBuyPrice);
        System.out.println("===================================");

        // Update all fields
        this.quantity = totalNewQuantity;
        this.averageBuyPrice = newAverageBuyPrice;
        this.totalInvested = newTotalInvested;
        this.lastUpdated = LocalDateTime.now();
    }

    /**
     * 🔧 PERFECT: Reduce quantity for sale with proper cost basis adjustment
     */
    public void reduceSale(BigDecimal soldQuantity) {
        if (soldQuantity.compareTo(this.quantity) > 0) {
            throw new IllegalArgumentException("Cannot sell more than available quantity");
        }

        // 🔧 PERFECT CALCULATION: Use average cost method
        // Calculate the cost basis of the sold portion using average cost
        BigDecimal averageCostPerUnit = this.totalInvested.divide(this.quantity, 8, RoundingMode.HALF_UP);
        BigDecimal soldCostBasis = soldQuantity.multiply(averageCostPerUnit);

        // 🔧 PERFECT DEBUG LOGGING
        System.out.println("=== PERFECT SALE REDUCTION ===");
        System.out.println("Current quantity: " + this.quantity);
        System.out.println("Current total invested: " + this.totalInvested);
        System.out.println("Sold quantity: " + soldQuantity);
        System.out.println("Average cost per unit: " + averageCostPerUnit);
        System.out.println("Cost basis of sold portion: " + soldCostBasis);

        // Update quantity and total invested
        this.quantity = this.quantity.subtract(soldQuantity);
        this.totalInvested = this.totalInvested.subtract(soldCostBasis);

        // Average buy price remains the same (cost per unit doesn't change)
        // Only quantity and total invested change

        System.out.println("New quantity: " + this.quantity);
        System.out.println("New total invested: " + this.totalInvested);
        System.out.println("==============================");

        this.lastUpdated = LocalDateTime.now();
    }

    /**
     * Calculate current value at given price
     */
    public BigDecimal getCurrentValue(BigDecimal currentPrice) {
        return quantity.multiply(currentPrice);
    }

    /**
     * 🔧 PERFECT: Calculate unrealized P&L using proper cost basis
     * Now uses totalInvested which includes fees, giving accurate P&L calculation
     */
    public BigDecimal calculateUnrealizedProfitLoss(BigDecimal currentPrice) {
        BigDecimal currentValue = getCurrentValue(currentPrice);
        return currentValue.subtract(totalInvested);
    }

    /**
     * Check if holding has positive quantity
     */
    public boolean hasPositiveQuantity() {
        return quantity != null && quantity.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * 🔧 PERFECT: Get true cost per unit including fees
     * This is useful for debugging and validation purposes
     */
    public BigDecimal getTrueCostPerUnit() {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return totalInvested.divide(quantity, 8, RoundingMode.HALF_UP);
    }

    /**
     * 🔧 PERFECT: Calculate profit/loss percentage
     */
    public BigDecimal calculateProfitLossPercentage(BigDecimal currentPrice) {
        if (totalInvested.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal unrealizedPnL = calculateUnrealizedProfitLoss(currentPrice);
        return unrealizedPnL.divide(totalInvested, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserHolding that = (UserHolding) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(userId, that.userId) &&
                Objects.equals(cryptoId, that.cryptoId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, userId, cryptoId);
    }

    @Override
    public String toString() {
        return "UserHolding{" +
                "id=" + id +
                ", userId=" + userId +
                ", cryptoId=" + cryptoId +
                ", quantity=" + quantity +
                ", averageBuyPrice=" + averageBuyPrice +
                ", totalInvested=" + totalInvested +
                ", trueCostPerUnit=" + getTrueCostPerUnit() +
                '}';
    }
}