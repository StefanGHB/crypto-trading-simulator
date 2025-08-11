package com.cryptotrading.crypto_trading_sim.model.enums;

/**
 * Enum representing the type of transaction (BUY or SELL)
 */
public enum TransactionType {
    BUY("Buy"),
    SELL("Sell");

    private final String displayName;

    TransactionType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}