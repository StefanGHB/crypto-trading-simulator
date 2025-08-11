package com.cryptotrading.crypto_trading_sim.controller;

import com.cryptotrading.crypto_trading_sim.model.Cryptocurrency;
import com.cryptotrading.crypto_trading_sim.service.CryptocurrencyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST Controller for Cryptocurrency-related operations
 * Clean and simple endpoints that work directly with Service layer
 */
@RestController
@RequestMapping("/api/cryptocurrencies")
public class CryptocurrencyController {

    @Autowired
    private CryptocurrencyService cryptocurrencyService;

    // ===============================================
    // CRYPTOCURRENCY DATA ENDPOINTS
    // ===============================================

    /**
     * Get all active cryptocurrencies
     * GET /api/cryptocurrencies
     */
    @GetMapping
    public ResponseEntity<?> getAllActiveCryptocurrencies() {
        try {
            List<Cryptocurrency> cryptos = cryptocurrencyService.getAllActiveCryptocurrencies();
            return ResponseEntity.ok(cryptos);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get cryptocurrencies: " + e.getMessage()));
        }
    }

    /**
     * Get top N cryptocurrencies by market cap rank
     * GET /api/cryptocurrencies/top?limit={limit}
     */
    @GetMapping("/top")
    public ResponseEntity<?> getTopCryptocurrencies(@RequestParam(defaultValue = "20") int limit) {
        try {
            List<Cryptocurrency> topCryptos = cryptocurrencyService.getTopCryptocurrencies(limit);
            return ResponseEntity.ok(topCryptos);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get top cryptocurrencies: " + e.getMessage()));
        }
    }

    /**
     * Get cryptocurrency by ID
     * GET /api/cryptocurrencies/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getCryptocurrencyById(@PathVariable Long id) {
        try {
            Optional<Cryptocurrency> crypto = cryptocurrencyService.getCryptocurrencyById(id);
            if (crypto.isPresent()) {
                return ResponseEntity.ok(crypto.get());
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Cryptocurrency not found"));
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get cryptocurrency: " + e.getMessage()));
        }
    }

    /**
     * Get cryptocurrency by symbol
     * GET /api/cryptocurrencies/symbol/{symbol}
     */
    @GetMapping("/symbol/{symbol}")
    public ResponseEntity<?> getCryptocurrencyBySymbol(@PathVariable String symbol) {
        try {
            Optional<Cryptocurrency> crypto = cryptocurrencyService.getCryptocurrencyBySymbol(symbol);
            if (crypto.isPresent()) {
                return ResponseEntity.ok(crypto.get());
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Cryptocurrency not found with symbol: " + symbol));
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get cryptocurrency: " + e.getMessage()));
        }
    }

    /**
     * Create new cryptocurrency
     * POST /api/cryptocurrencies
     */
    @PostMapping
    public ResponseEntity<?> createCryptocurrency(@RequestBody Map<String, Object> request) {
        try {
            String symbol = (String) request.get("symbol");
            String name = (String) request.get("name");
            String krakenPairName = (String) request.get("krakenPairName");
            Integer marketCapRank = Integer.valueOf(request.get("marketCapRank").toString());

            Cryptocurrency newCrypto = cryptocurrencyService.createCryptocurrency(symbol, name, krakenPairName, marketCapRank);
            return ResponseEntity.status(HttpStatus.CREATED).body(newCrypto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create cryptocurrency: " + e.getMessage()));
        }
    }

    /**
     * Update cryptocurrency information
     * PUT /api/cryptocurrencies/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateCryptocurrency(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        try {
            Optional<Cryptocurrency> existingCrypto = cryptocurrencyService.getCryptocurrencyById(id);
            if (existingCrypto.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Cryptocurrency not found"));
            }

            Cryptocurrency crypto = existingCrypto.get();
            crypto.setSymbol((String) request.get("symbol"));
            crypto.setName((String) request.get("name"));
            crypto.setKrakenPairName((String) request.get("krakenPairName"));
            crypto.setMarketCapRank(Integer.valueOf(request.get("marketCapRank").toString()));
            crypto.setActive(Boolean.valueOf(request.get("active").toString()));

            Cryptocurrency updatedCrypto = cryptocurrencyService.updateCryptocurrency(crypto);
            return ResponseEntity.ok(updatedCrypto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update cryptocurrency: " + e.getMessage()));
        }
    }

    // ===============================================
    // PRICE MANAGEMENT ENDPOINTS
    // ===============================================

    /**
     * Get current price for cryptocurrency by symbol
     * GET /api/cryptocurrencies/{symbol}/price
     */
    @GetMapping("/{symbol}/price")
    public ResponseEntity<?> getCurrentPrice(@PathVariable String symbol) {
        try {
            BigDecimal currentPrice = cryptocurrencyService.getCurrentPrice(symbol);
            return ResponseEntity.ok(Map.of(
                    "symbol", symbol,
                    "currentPrice", currentPrice
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get price: " + e.getMessage()));
        }
    }

    /**
     * Get all current prices
     * GET /api/cryptocurrencies/prices
     */
    @GetMapping("/prices")
    public ResponseEntity<?> getAllCurrentPrices() {
        try {
            Map<String, BigDecimal> prices = cryptocurrencyService.getAllCurrentPrices();
            return ResponseEntity.ok(prices);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get prices: " + e.getMessage()));
        }
    }

    /**
     * Update cryptocurrency price
     * PUT /api/cryptocurrencies/{symbol}/price
     */
    @PutMapping("/{symbol}/price")
    public ResponseEntity<?> updatePrice(@PathVariable String symbol, @RequestBody Map<String, Object> request) {
        try {
            BigDecimal price = new BigDecimal(request.get("price").toString());
            boolean updated = cryptocurrencyService.updatePrice(symbol, price);

            if (updated) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Price for " + symbol + " updated to $" + price
                ));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Failed to update price"));
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update price: " + e.getMessage()));
        }
    }

    /**
     * Update cryptocurrency price with 24h changes
     * PUT /api/cryptocurrencies/{symbol}/price-with-changes
     */
    @PutMapping("/{symbol}/price-with-changes")
    public ResponseEntity<?> updatePriceWithChanges(@PathVariable String symbol, @RequestBody Map<String, Object> request) {
        try {
            BigDecimal price = new BigDecimal(request.get("price").toString());
            BigDecimal priceChange24h = request.get("priceChange24h") != null ?
                    new BigDecimal(request.get("priceChange24h").toString()) : null;
            BigDecimal priceChangePercent24h = request.get("priceChangePercent24h") != null ?
                    new BigDecimal(request.get("priceChangePercent24h").toString()) : null;

            boolean updated = cryptocurrencyService.updatePriceWithChanges(symbol, price, priceChange24h, priceChangePercent24h);

            if (updated) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Price data for " + symbol + " updated"
                ));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Failed to update price data"));
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update price data: " + e.getMessage()));
        }
    }

    /**
     * Batch update prices for multiple cryptocurrencies
     * PUT /api/cryptocurrencies/batch-update-prices
     */
    @PutMapping("/batch-update-prices")
    public ResponseEntity<?> batchUpdatePrices(@RequestBody List<Cryptocurrency> cryptosWithNewPrices) {
        try {
            cryptocurrencyService.batchUpdatePrices(cryptosWithNewPrices);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Updated prices for " + cryptosWithNewPrices.size() + " cryptocurrencies"
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to batch update prices: " + e.getMessage()));
        }
    }

    // ===============================================
    // MARKET ANALYSIS ENDPOINTS
    // ===============================================

    /**
     * Get market summary
     * GET /api/cryptocurrencies/market-summary
     */
    @GetMapping("/market-summary")
    public ResponseEntity<?> getMarketSummary() {
        try {
            CryptocurrencyService.MarketSummary marketSummary = cryptocurrencyService.getMarketSummary();
            return ResponseEntity.ok(marketSummary);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get market summary: " + e.getMessage()));
        }
    }

    /**
     * Get cryptocurrencies with significant price changes (>5%)
     * GET /api/cryptocurrencies/significant-changes
     */
    @GetMapping("/significant-changes")
    public ResponseEntity<?> getCryptocurrenciesWithSignificantChanges() {
        try {
            List<Cryptocurrency> cryptosWithChanges = cryptocurrencyService.getCryptocurrenciesWithSignificantChanges();
            return ResponseEntity.ok(cryptosWithChanges);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get significant changes: " + e.getMessage()));
        }
    }

    /**
     * Get top gainers
     * GET /api/cryptocurrencies/top-gainers?limit={limit}
     */
    @GetMapping("/top-gainers")
    public ResponseEntity<?> getTopGainers(@RequestParam(defaultValue = "10") int limit) {
        try {
            List<Cryptocurrency> topGainers = cryptocurrencyService.getTopGainers(limit);
            return ResponseEntity.ok(topGainers);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get top gainers: " + e.getMessage()));
        }
    }

    /**
     * Get top losers
     * GET /api/cryptocurrencies/top-losers?limit={limit}
     */
    @GetMapping("/top-losers")
    public ResponseEntity<?> getTopLosers(@RequestParam(defaultValue = "10") int limit) {
        try {
            List<Cryptocurrency> topLosers = cryptocurrencyService.getTopLosers(limit);
            return ResponseEntity.ok(topLosers);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get top losers: " + e.getMessage()));
        }
    }

    // ===============================================
    // UTILITY ENDPOINTS
    // ===============================================

    /**
     * Get all active Kraken trading pairs
     * GET /api/cryptocurrencies/kraken-pairs
     */
    @GetMapping("/kraken-pairs")
    public ResponseEntity<?> getAllActiveKrakenPairs() {
        try {
            List<String> krakenPairs = cryptocurrencyService.getAllActiveKrakenPairs();
            return ResponseEntity.ok(krakenPairs);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get Kraken pairs: " + e.getMessage()));
        }
    }

    /**
     * Check if cryptocurrency exists by symbol
     * GET /api/cryptocurrencies/exists?symbol={symbol}
     */
    @GetMapping("/exists")
    public ResponseEntity<?> checkCryptocurrencyExists(@RequestParam String symbol) {
        try {
            boolean exists = cryptocurrencyService.existsBySymbol(symbol);
            return ResponseEntity.ok(Map.of(
                    "symbol", symbol,
                    "exists", exists
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to check existence: " + e.getMessage()));
        }
    }

    /**
     * Get cryptocurrency ID by symbol
     * GET /api/cryptocurrencies/symbol/{symbol}/id
     */
    @GetMapping("/symbol/{symbol}/id")
    public ResponseEntity<?> getCryptocurrencyIdBySymbol(@PathVariable String symbol) {
        try {
            Long cryptoId = cryptocurrencyService.getCryptocurrencyIdBySymbol(symbol);
            return ResponseEntity.ok(Map.of(
                    "symbol", symbol,
                    "id", cryptoId
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get cryptocurrency ID: " + e.getMessage()));
        }
    }

    /**
     * Deactivate cryptocurrency
     * POST /api/cryptocurrencies/{id}/deactivate
     */
    @PostMapping("/{id}/deactivate")
    public ResponseEntity<?> deactivateCryptocurrency(@PathVariable Long id) {
        try {
            boolean deactivated = cryptocurrencyService.deactivateCryptocurrency(id);
            if (deactivated) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Cryptocurrency deactivated successfully"
                ));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Failed to deactivate cryptocurrency"));
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to deactivate cryptocurrency: " + e.getMessage()));
        }
    }
}