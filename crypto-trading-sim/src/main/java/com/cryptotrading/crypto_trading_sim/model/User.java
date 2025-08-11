package com.cryptotrading.crypto_trading_sim.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * User entity representing a user account in the crypto trading simulator
 */
public class User {
    private Long id;
    private String username;
    private String email;
    private BigDecimal initialBalance;
    private BigDecimal currentBalance;
    private BigDecimal totalInvested;
    private BigDecimal totalProfitLoss;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean isActive;

    // Default constructor
    public User() {}

    // Constructor for new user
    public User(String username, String email, BigDecimal initialBalance) {
        this.username = username;
        this.email = email;
        this.initialBalance = initialBalance;
        this.currentBalance = initialBalance;
        this.totalInvested = BigDecimal.ZERO;
        this.totalProfitLoss = BigDecimal.ZERO;
        this.isActive = true;
    }

    // Full constructor
    public User(Long id, String username, String email, BigDecimal initialBalance,
                BigDecimal currentBalance, BigDecimal totalInvested, BigDecimal totalProfitLoss,
                LocalDateTime createdAt, LocalDateTime updatedAt, boolean isActive) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.initialBalance = initialBalance;
        this.currentBalance = currentBalance;
        this.totalInvested = totalInvested;
        this.totalProfitLoss = totalProfitLoss;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.isActive = isActive;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public BigDecimal getInitialBalance() { return initialBalance; }
    public void setInitialBalance(BigDecimal initialBalance) { this.initialBalance = initialBalance; }

    public BigDecimal getCurrentBalance() { return currentBalance; }
    public void setCurrentBalance(BigDecimal currentBalance) { this.currentBalance = currentBalance; }

    public BigDecimal getTotalInvested() { return totalInvested; }
    public void setTotalInvested(BigDecimal totalInvested) { this.totalInvested = totalInvested; }

    public BigDecimal getTotalProfitLoss() { return totalProfitLoss; }
    public void setTotalProfitLoss(BigDecimal totalProfitLoss) { this.totalProfitLoss = totalProfitLoss; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    // Business methods
    public boolean canAfford(BigDecimal amount) {
        return currentBalance.compareTo(amount) >= 0;
    }

    public void deductBalance(BigDecimal amount) {
        this.currentBalance = this.currentBalance.subtract(amount);
    }

    public void addBalance(BigDecimal amount) {
        this.currentBalance = this.currentBalance.add(amount);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id) && Objects.equals(username, user.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, username);
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", currentBalance=" + currentBalance +
                ", totalProfitLoss=" + totalProfitLoss +
                '}';
    }
}