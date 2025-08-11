package com.cryptotrading.crypto_trading_sim.service;

import com.cryptotrading.crypto_trading_sim.dao.CryptocurrencyDao;
import com.cryptotrading.crypto_trading_sim.model.Cryptocurrency;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ===============================================
 * 🎯 ФИНАЛЕН KRAKEN WEBSOCKET SERVICE - ТОЧНИ ТОП 20
 * ===============================================
 * ⚡ МАКСИМАЛНО ОПТИМИЗИРАНО за INSTANT real-time sync
 * 🚀 ZERO DELAYS - БЕЗ филтри за максимална скорост
 * 📋 ТОЧНИ ТОП 20 криптовалути според coding task
 */
@Service
public class KrakenWebSocketService {

    private static final Logger logger = LoggerFactory.getLogger(KrakenWebSocketService.class);

    @Autowired
    private CryptocurrencyDao cryptocurrencyDao;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Value("${kraken.websocket.url:wss://ws.kraken.com/v2}")
    private String krakenWebSocketUrl;

    @Value("${kraken.api.reconnect.attempts:20}")
    private int maxReconnectAttempts;

    @Value("${kraken.api.reconnect.delay:1000}")
    private long reconnectDelayMs;

    private WebSocketClient webSocketClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicBoolean shouldReconnect = new AtomicBoolean(true);
    private final AtomicLong lastConnectionAttempt = new AtomicLong(0);
    private final AtomicLong lastHeartbeat = new AtomicLong(System.currentTimeMillis());
    private int reconnectAttempts = 0;
    private ScheduledExecutorService scheduler;
    private ScheduledExecutorService reconnectScheduler;

    // ⚡ STATISTICS - НЕ БЛОКИРА UPDATES
    private final Map<String, BigDecimal> lastPriceStats = new ConcurrentHashMap<>();
    private final AtomicLong totalUpdatesProcessed = new AtomicLong(0);
    private final AtomicLong lastPriceUpdateTime = new AtomicLong(0);

    // Real-time optimized settings
    private static final long MIN_CONNECTION_INTERVAL = 1000; // 1 секунда
    private static final long RATE_LIMIT_BACKOFF = 5000; // 5 секунди за rate limit
    private static final long HEARTBEAT_INTERVAL = 2000; // 2 секунди heartbeat
    private static final long CONNECTION_TIMEOUT = 8000; // 8 секунди connection timeout
    private static final long HEALTH_CHECK_INTERVAL = 3000; // 3 секунди health check

    // Map of Kraken pair names to our symbols
    private Map<String, String> krakenPairToSymbolMap;
    private Set<String> subscribedPairs;

    // ===============================================
    // 🎯 ТОЧНИ ТОП 20 КРИПТОВАЛУТИ СПОРЕД CODING TASK
    // ===============================================
    private static final Map<String, String> TOP_20_KRAKEN_PAIRS = Map.ofEntries(
            // ===============================================
            // ТОП 1-10 - ОСНОВНИ КРИПТОВАЛУТИ (ВСИЧКИ ✅ ВАЛИДНИ)
            // ===============================================
            Map.entry("BTC", "BTC/USD"),      // #1 Bitcoin - King crypto ✅
            Map.entry("ETH", "ETH/USD"),      // #2 Ethereum - Smart contracts ✅
            Map.entry("XRP", "XRP/USD"),      // #3 XRP - Cross-border payments ✅
            Map.entry("USDT", "USDT/USD"),    // #4 Tether - Major stablecoin ✅
            Map.entry("BNB", "BNB/USD"),      // #5 BNB - Binance Chain ✅ НАЛИЧНО!
            Map.entry("SOL", "SOL/USD"),      // #6 Solana - High performance ✅
            Map.entry("USDC", "USDC/USD"),    // #7 USD Coin - Regulated stablecoin ✅
            Map.entry("DOGE", "DOGE/USD"),    // #8 Dogecoin - Community-driven ✅
            Map.entry("TRX", "TRX/USD"),      // #9 TRON - Entertainment platform ✅
            Map.entry("ADA", "ADA/USD"),      // #10 Cardano - Academic blockchain ✅

            // ===============================================
            // ТОП 11-20 - ДОПЪЛНИТЕЛНИ КРИПТОВАЛУТИ (ВСИЧКИ ✅ ВАЛИДНИ)
            // ===============================================
            Map.entry("ALGO", "ALGO/USD"),    // #11 Algorand - Pure Proof of Stake ✅
            Map.entry("XLM", "XLM/USD"),      // #12 Stellar - Cross-border transfers ✅
            Map.entry("SUI", "SUI/USD"),      // #13 Sui Network ✅ НАЛИЧНО!
            Map.entry("LINK", "LINK/USD"),    // #14 Chainlink - Oracle network ✅
            Map.entry("BCH", "BCH/USD"),      // #15 Bitcoin Cash - Bitcoin fork ✅
            Map.entry("HBAR", "HBAR/USD"),    // #16 Hedera - Enterprise blockchain ✅
            Map.entry("AVAX", "AVAX/USD"),    // #17 Avalanche - Fast consensus ✅
            Map.entry("LTC", "LTC/USD"),      // #18 Litecoin - Digital silver ✅
            Map.entry("TON", "TON/USD"),      // #19 TonCoin ✅ НАЛИЧНО!
            Map.entry("USDS", "USDS/USD")     // #20 USDS - Sky Dollar ✅ НАЛИЧНО!
    );

    @PostConstruct
    public void initialize() {
        logger.info("🎯 Initializing FINAL TOP 20 Kraken WebSocket Service...");
        logger.info("⚡ MAXIMUM SPEED MODE - ALL FILTERS REMOVED");
        logger.info("📋 EXACT TOP 20 cryptocurrencies according to coding task");

        // Optimized schedulers
        scheduler = Executors.newScheduledThreadPool(2);
        reconnectScheduler = Executors.newScheduledThreadPool(1);

        // Initialize pair mappings
        initializePairMappings();

        // Connect to Kraken
        connectToKraken();
    }

    @PreDestroy
    public void shutdown() {
        logger.info("🛑 Shutting down TOP 20 Kraken WebSocket Service...");
        shouldReconnect.set(false);

        if (webSocketClient != null && webSocketClient.isOpen()) {
            webSocketClient.close();
        }

        shutdownSchedulers();
    }

    // ===============================================
    // WEBSOCKET CONNECTION
    // ===============================================

    private void connectToKraken() {
        long now = System.currentTimeMillis();
        long timeSinceLastAttempt = now - lastConnectionAttempt.get();

        if (timeSinceLastAttempt < MIN_CONNECTION_INTERVAL) {
            long waitTime = MIN_CONNECTION_INTERVAL - timeSinceLastAttempt;
            logger.debug("⏱️ Rate limiting connection: waiting {} ms", waitTime);
            scheduler.schedule(this::connectToKraken, waitTime, TimeUnit.MILLISECONDS);
            return;
        }

        lastConnectionAttempt.set(now);

        try {
            logger.info("🔌 Connecting to Kraken WebSocket: {} (attempt {})", krakenWebSocketUrl, reconnectAttempts + 1);

            URI serverUri = URI.create(krakenWebSocketUrl);
            Draft draft = new Draft_6455();

            webSocketClient = new WebSocketClient(serverUri, draft) {
                @Override
                public void onOpen(ServerHandshake handshake) {
                    handleConnectionOpen(handshake);
                }

                @Override
                public void onMessage(String message) {
                    handleMessageInstant(message);
                }

                @Override
                public void onMessage(ByteBuffer message) {
                    handleMessageInstant(new String(message.array()));
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    handleConnectionClose(code, reason, remote);
                }

                @Override
                public void onError(Exception ex) {
                    handleConnectionError(ex);
                }
            };

            // Connect with optimized timeout
            CompletableFuture.runAsync(() -> {
                try {
                    webSocketClient.connectBlocking(CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS);
                } catch (Exception e) {
                    logger.error("❌ Connection failed", e);
                    scheduleIntelligentReconnect();
                }
            });

        } catch (Exception e) {
            logger.error("❌ Failed to create WebSocket connection", e);
            scheduleIntelligentReconnect();
        }
    }

    private void handleConnectionOpen(ServerHandshake handshake) {
        logger.info("✅ Connected to Kraken WebSocket successfully - TOP 20 MODE");
        connected.set(true);
        reconnectAttempts = 0;
        lastHeartbeat.set(System.currentTimeMillis());

        // Clear stats on new connection
        lastPriceStats.clear();
        totalUpdatesProcessed.set(0);

        // Subscribe to price updates
        subscribeToTicker();

        // Start health monitoring
        startRealTimeHealthCheck();
    }

    // ===============================================
    // ⚡ INSTANT MESSAGE PROCESSING - NO FILTERS
    // ===============================================

    private void handleMessageInstant(String message) {
        try {
            if (message == null || message.trim().isEmpty()) {
                return;
            }

            lastHeartbeat.set(System.currentTimeMillis());

            // Parse JSON immediately
            JsonNode rootNode;
            try {
                rootNode = objectMapper.readTree(message);
            } catch (Exception jsonException) {
                logger.warn("⚠️ Failed to parse JSON: {}", message.length() > 200 ? message.substring(0, 200) + "..." : message);
                return;
            }

            // Handle different message types
            if (rootNode.has("method")) {
                String method = rootNode.get("method").asText();
                if ("subscribe".equals(method)) {
                    handleSubscriptionConfirmation(rootNode);
                }
            } else if (rootNode.has("channel")) {
                JsonNode channelNode = rootNode.get("channel");
                if (channelNode != null && "ticker".equals(channelNode.asText())) {
                    // ⚡ INSTANT TICKER PROCESSING - NO DEDUPLICATION
                    handleTickerUpdateInstant(rootNode);
                }
            } else if (rootNode.has("type")) {
                JsonNode typeNode = rootNode.get("type");
                if (typeNode != null && "heartbeat".equals(typeNode.asText())) {
                    handleHeartbeat(rootNode);
                }
            } else if (rootNode.has("event")) {
                JsonNode eventNode = rootNode.get("event");
                if (eventNode != null && "subscriptionStatus".equals(eventNode.asText())) {
                    handleSubscriptionConfirmation(rootNode);
                }
            }

        } catch (Exception e) {
            logger.error("❌ Error processing message", e);
        }
    }

    // ===============================================
    // ⚡ INSTANT TICKER PROCESSING - ZERO FILTERS
    // ===============================================

    private void handleTickerUpdateInstant(JsonNode rootNode) {
        try {
            if (!rootNode.has("data")) {
                return;
            }

            JsonNode dataArray = rootNode.get("data");
            if (!dataArray.isArray()) {
                return;
            }

            // Process each ticker update INSTANTLY
            for (JsonNode tickerData : dataArray) {
                processTickerInstant(tickerData);
            }

        } catch (Exception e) {
            logger.error("❌ Error handling ticker update", e);
        }
    }

    private void processTickerInstant(JsonNode tickerData) {
        try {
            if (!tickerData.has("symbol") || !tickerData.has("last")) {
                return;
            }

            String krakenPair = tickerData.get("symbol").asText();
            String ourSymbol = krakenPairToSymbolMap.get(krakenPair);

            if (ourSymbol == null) {
                return;
            }

            BigDecimal newPrice = new BigDecimal(tickerData.get("last").asText());
            long currentTime = System.currentTimeMillis();

            // ===============================================
            // ⚡ ZERO FILTERS - INSTANT PROCESSING
            // ===============================================

            // САМО BASIC VALIDATION
            if (newPrice == null || newPrice.compareTo(BigDecimal.ZERO) <= 0) {
                logger.warn("⚠️ Invalid price for {}: {}", ourSymbol, newPrice);
                return;
            }

            // Extract additional data
            BigDecimal change24h = null;
            BigDecimal changePercent24h = null;

            if (tickerData.has("change")) {
                try {
                    change24h = new BigDecimal(tickerData.get("change").asText());
                } catch (Exception e) {
                    // Ignore parsing errors for optional fields
                }
            }
            if (tickerData.has("change_pct")) {
                try {
                    changePercent24h = new BigDecimal(tickerData.get("change_pct").asText());
                } catch (Exception e) {
                    // Ignore parsing errors for optional fields
                }
            }

            // ⚡ LOG EVERY UPDATE FOR MAXIMUM TRANSPARENCY
            BigDecimal lastPrice = lastPriceStats.get(ourSymbol);
            if (lastPrice == null || lastPrice.compareTo(newPrice) != 0) {
                logger.info("⚡ INSTANT update for {}: ${} (was: ${})",
                        ourSymbol, formatPrice(newPrice), lastPrice != null ? formatPrice(lastPrice) : "N/A");
            } else {
                logger.debug("🔄 Same price update for {}: ${}", ourSymbol, formatPrice(newPrice));
            }

            // Update stats (non-blocking)
            lastPriceStats.put(ourSymbol, newPrice);
            totalUpdatesProcessed.incrementAndGet();

            // ⚡ INSTANT DATABASE UPDATE - NO DELAYS
            boolean updated = cryptocurrencyDao.updatePriceWithChanges(
                    ourSymbol, newPrice, change24h, changePercent24h);

            if (updated) {
                // ⚡ INSTANT BROADCAST - NO DELAYS
                broadcastInstantPriceUpdate(ourSymbol, newPrice, change24h, changePercent24h);
                lastPriceUpdateTime.set(currentTime);
            } else {
                logger.warn("⚠️ Failed to update database for {}", ourSymbol);
            }

        } catch (Exception e) {
            logger.error("❌ Error processing ticker for {}: {}",
                    tickerData.has("symbol") ? tickerData.get("symbol").asText() : "unknown", e.getMessage());
        }
    }

    // ===============================================
    // SUBSCRIPTION MANAGEMENT
    // ===============================================

    private void subscribeToTicker() {
        try {
            List<String> krakenPairs = new ArrayList<>(TOP_20_KRAKEN_PAIRS.values());
            subscribedPairs = new HashSet<>(krakenPairs);

            logger.info("📡 Subscribing to {} TOP 20 pairs for INSTANT updates", krakenPairs.size());

            Map<String, Object> subscribeMessage = new HashMap<>();
            subscribeMessage.put("method", "subscribe");

            Map<String, Object> params = new HashMap<>();
            params.put("channel", "ticker");
            params.put("symbol", krakenPairs);
            subscribeMessage.put("params", params);

            String messageJson = objectMapper.writeValueAsString(subscribeMessage);

            if (webSocketClient != null && webSocketClient.isOpen()) {
                webSocketClient.send(messageJson);
                logger.info("✅ Subscribed to TOP 20 INSTANT real-time ticker");
            }

        } catch (Exception e) {
            logger.error("❌ Failed to subscribe to ticker", e);
        }
    }

    private void handleSubscriptionConfirmation(JsonNode rootNode) {
        try {
            logger.debug("📋 Subscription confirmation: {}", rootNode.toString());

            if (rootNode.has("result")) {
                JsonNode result = rootNode.get("result");
                JsonNode statusNode = result.get("status");
                if (statusNode != null && "subscribed".equals(statusNode.asText())) {
                    JsonNode channelNode = result.get("channel");
                    String channelName = channelNode != null ? channelNode.asText() : "unknown";
                    logger.info("✅ Successfully subscribed to channel: {} - TOP 20 MODE", channelName);
                }
            } else if (rootNode.has("success") && rootNode.get("success").asBoolean()) {
                logger.info("✅ Successfully subscribed to TOP 20 Kraken WebSocket");
            }
        } catch (Exception e) {
            logger.error("❌ Error handling subscription confirmation: {}", rootNode.toString(), e);
        }
    }

    // ===============================================
    // ⚡ INSTANT WEBSOCKET BROADCASTING
    // ===============================================

    private void broadcastInstantPriceUpdate(String symbol, BigDecimal price,
                                             BigDecimal change24h, BigDecimal changePercent24h) {
        try {
            // ⚡ INSTANT individual update for maximum responsiveness
            Map<String, Object> priceUpdate = new HashMap<>();
            priceUpdate.put("symbol", symbol);
            priceUpdate.put("price", price);
            priceUpdate.put("change24h", change24h);
            priceUpdate.put("changePercent24h", changePercent24h);
            priceUpdate.put("timestamp", System.currentTimeMillis());

            // ⚡ INSTANT broadcast - NO DELAYS
            messagingTemplate.convertAndSend("/topic/prices", priceUpdate);

            logger.debug("⚡ INSTANT broadcast: {} = ${}", symbol, formatPrice(price));

        } catch (Exception e) {
            logger.error("❌ Error broadcasting instant update", e);
        }
    }

    // ===============================================
    // CONNECTION MANAGEMENT
    // ===============================================

    private void handleConnectionClose(int code, String reason, boolean remote) {
        logger.warn("⚠️ WebSocket closed. Code: {}, Reason: {}, Remote: {}", code, reason, remote);
        connected.set(false);

        if (reason != null && reason.contains("429")) {
            logger.error("🚨 Rate limited! Waiting {} ms", RATE_LIMIT_BACKOFF);
            scheduler.schedule(() -> {
                if (shouldReconnect.get()) {
                    connectToKraken();
                }
            }, RATE_LIMIT_BACKOFF, TimeUnit.MILLISECONDS);
            return;
        }

        if (shouldReconnect.get()) {
            scheduleIntelligentReconnect();
        }
    }

    private void handleConnectionError(Exception ex) {
        logger.error("❌ WebSocket connection error", ex);
        connected.set(false);

        if (shouldReconnect.get()) {
            scheduleIntelligentReconnect();
        }
    }

    private void scheduleIntelligentReconnect() {
        if (!shouldReconnect.get()) {
            return;
        }

        reconnectAttempts++;

        if (reconnectAttempts > maxReconnectAttempts) {
            logger.error("❌ Max reconnect attempts reached. Resetting...");
            reconnectAttempts = 0;
            scheduler.schedule(() -> {
                if (shouldReconnect.get()) {
                    connectToKraken();
                }
            }, 30, TimeUnit.SECONDS);
            return;
        }

        // Fast reconnect for real-time performance
        long delay = Math.min(reconnectDelayMs * reconnectAttempts, 3000);
        delay = Math.max(delay, MIN_CONNECTION_INTERVAL);

        logger.info("🔄 TOP 20 reconnect in {} ms (attempt {})", delay, reconnectAttempts);

        reconnectScheduler.schedule(() -> {
            if (shouldReconnect.get()) {
                connectToKraken();
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    private void startRealTimeHealthCheck() {
        scheduler.scheduleAtFixedRate(() -> {
            long now = System.currentTimeMillis();
            long timeSinceHeartbeat = now - lastHeartbeat.get();

            if (timeSinceHeartbeat > HEARTBEAT_INTERVAL * 3) {
                logger.warn("💓 Stale connection detected ({}ms), forcing reconnect", timeSinceHeartbeat);

                if (connected.get() && shouldReconnect.get()) {
                    if (webSocketClient != null && webSocketClient.isOpen()) {
                        webSocketClient.close();
                    }
                }
            }

            if (!connected.get() && shouldReconnect.get()) {
                scheduleIntelligentReconnect();
            }
        }, HEALTH_CHECK_INTERVAL, HEALTH_CHECK_INTERVAL, TimeUnit.MILLISECONDS);
    }

    private void handleHeartbeat(JsonNode rootNode) {
        logger.debug("💓 Heartbeat from Kraken");
        lastHeartbeat.set(System.currentTimeMillis());
    }

    // ===============================================
    // UTILITY METHODS
    // ===============================================

    private void initializePairMappings() {
        krakenPairToSymbolMap = new HashMap<>();
        TOP_20_KRAKEN_PAIRS.forEach((symbol, krakenPair) -> {
            krakenPairToSymbolMap.put(krakenPair, symbol);
        });
        logger.info("✅ Initialized {} TOP 20 pair mappings for INSTANT sync", krakenPairToSymbolMap.size());
    }

    private String formatPrice(BigDecimal price) {
        if (price == null) return "0.00";
        if (price.compareTo(BigDecimal.ONE) >= 0) {
            return String.format("%.2f", price);
        } else {
            return String.format("%.8f", price);
        }
    }

    private void shutdownSchedulers() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
        if (reconnectScheduler != null && !reconnectScheduler.isShutdown()) {
            reconnectScheduler.shutdown();
        }
    }

    // ===============================================
    // PUBLIC API METHODS
    // ===============================================

    public boolean isConnected() {
        return connected.get() && webSocketClient != null && webSocketClient.isOpen();
    }

    public ConnectionStatus getConnectionStatus() {
        long timeSinceHeartbeat = System.currentTimeMillis() - lastHeartbeat.get();
        long timeSinceUpdate = System.currentTimeMillis() - lastPriceUpdateTime.get();
        long totalUpdates = totalUpdatesProcessed.get();

        return new ConnectionStatus(
                isConnected(),
                reconnectAttempts,
                subscribedPairs != null ? subscribedPairs.size() : 0,
                lastHeartbeat.get(),
                timeSinceHeartbeat,
                timeSinceUpdate,
                totalUpdates
        );
    }

    public void forceReconnect() {
        logger.info("🔄 Force reconnect requested - TOP 20 MODE");
        if (webSocketClient != null && webSocketClient.isOpen()) {
            webSocketClient.close();
        }
        reconnectAttempts = 0;
        scheduler.schedule(this::connectToKraken, MIN_CONNECTION_INTERVAL, TimeUnit.MILLISECONDS);
    }

    public void triggerPriceUpdate() {
        logger.info("📊 Manual trigger - sending TOP 20 current prices");
        try {
            List<Cryptocurrency> cryptos = cryptocurrencyDao.findAllActive();
            for (Cryptocurrency crypto : cryptos) {
                broadcastInstantPriceUpdate(crypto.getSymbol(), crypto.getCurrentPrice(),
                        crypto.getPriceChange24h(), crypto.getPriceChangePercent24h());
            }
        } catch (Exception e) {
            logger.error("❌ Error during manual trigger", e);
        }
    }

    // ===============================================
    // ENHANCED CONNECTION STATUS CLASS
    // ===============================================

    public static class ConnectionStatus {
        private final boolean connected;
        private final int reconnectAttempts;
        private final int subscribedPairs;
        private final long lastHeartbeat;
        private final long timeSinceHeartbeat;
        private final long timeSinceLastUpdate;
        private final long totalUpdatesProcessed;

        public ConnectionStatus(boolean connected, int reconnectAttempts, int subscribedPairs,
                                long lastHeartbeat, long timeSinceHeartbeat, long timeSinceLastUpdate,
                                long totalUpdatesProcessed) {
            this.connected = connected;
            this.reconnectAttempts = reconnectAttempts;
            this.subscribedPairs = subscribedPairs;
            this.lastHeartbeat = lastHeartbeat;
            this.timeSinceHeartbeat = timeSinceHeartbeat;
            this.timeSinceLastUpdate = timeSinceLastUpdate;
            this.totalUpdatesProcessed = totalUpdatesProcessed;
        }

        // Getters
        public boolean isConnected() { return connected; }
        public int getReconnectAttempts() { return reconnectAttempts; }
        public int getSubscribedPairs() { return subscribedPairs; }
        public long getLastHeartbeat() { return lastHeartbeat; }
        public long getTimeSinceHeartbeat() { return timeSinceHeartbeat; }
        public long getTimeSinceLastUpdate() { return timeSinceLastUpdate; }
        public long getTotalUpdatesProcessed() { return totalUpdatesProcessed; }

        // Backward compatibility
        @Deprecated
        public long getLastUpdateTime() { return lastHeartbeat; }
    }
}