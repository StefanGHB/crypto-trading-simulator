package com.cryptotrading.crypto_trading_sim.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Cryptocurrency entity representing a digital currency in the trading platform
 */
public class Cryptocurrency {
    private Long id;
    private String symbol;
    private String name;
    private String krakenPairName;
    private BigDecimal currentPrice;
    private BigDecimal priceChange24h;
    private BigDecimal priceChangePercent24h;
    private Integer marketCapRank;
    private boolean isActive;
    private LocalDateTime lastPriceUpdate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Default constructor
    public Cryptocurrency() {}

    // Constructor for new cryptocurrency
    public Cryptocurrency(String symbol, String name, String krakenPairName, Integer marketCapRank) {
        this.symbol = symbol;
        this.name = name;
        this.krakenPairName = krakenPairName;
        this.marketCapRank = marketCapRank;
        this.currentPrice = BigDecimal.ZERO;
        this.priceChange24h = BigDecimal.ZERO;
        this.priceChangePercent24h = BigDecimal.ZERO;
        this.isActive = true;
    }

    // Full constructor
    public Cryptocurrency(Long id, String symbol, String name, String krakenPairName,
                          BigDecimal currentPrice, BigDecimal priceChange24h,
                          BigDecimal priceChangePercent24h, Integer marketCapRank,
                          boolean isActive, LocalDateTime lastPriceUpdate,
                          LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.symbol = symbol;
        this.name = name;
        this.krakenPairName = krakenPairName;
        this.currentPrice = currentPrice;
        this.priceChange24h = priceChange24h;
        this.priceChangePercent24h = priceChangePercent24h;
        this.marketCapRank = marketCapRank;
        this.isActive = isActive;
        this.lastPriceUpdate = lastPriceUpdate;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getKrakenPairName() { return krakenPairName; }
    public void setKrakenPairName(String krakenPairName) { this.krakenPairName = krakenPairName; }

    public BigDecimal getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
        this.lastPriceUpdate = LocalDateTime.now();
    }

    public BigDecimal getPriceChange24h() { return priceChange24h; }
    public void setPriceChange24h(BigDecimal priceChange24h) { this.priceChange24h = priceChange24h; }

    public BigDecimal getPriceChangePercent24h() { return priceChangePercent24h; }
    public void setPriceChangePercent24h(BigDecimal priceChangePercent24h) {
        this.priceChangePercent24h = priceChangePercent24h;
    }

    public Integer getMarketCapRank() { return marketCapRank; }
    public void setMarketCapRank(Integer marketCapRank) { this.marketCapRank = marketCapRank; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public LocalDateTime getLastPriceUpdate() { return lastPriceUpdate; }
    public void setLastPriceUpdate(LocalDateTime lastPriceUpdate) { this.lastPriceUpdate = lastPriceUpdate; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // Business methods
    public boolean isPricePositive() {
        return currentPrice != null && currentPrice.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isChangePositive() {
        return priceChange24h != null && priceChange24h.compareTo(BigDecimal.ZERO) > 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Cryptocurrency that = (Cryptocurrency) o;
        return Objects.equals(id, that.id) && Objects.equals(symbol, that.symbol);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, symbol);
    }

    @Override
    public String toString() {
        return "Cryptocurrency{" +
                "id=" + id +
                ", symbol='" + symbol + '\'' +
                ", name='" + name + '\'' +
                ", currentPrice=" + currentPrice +
                ", marketCapRank=" + marketCapRank +
                '}';
    }
}