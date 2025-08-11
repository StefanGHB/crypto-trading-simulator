package com.cryptotrading.crypto_trading_sim.controller;

import com.cryptotrading.crypto_trading_sim.service.CryptocurrencyService;
import com.cryptotrading.crypto_trading_sim.service.KrakenWebSocketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Combined WebSocket Controller for both real-time communication and REST management
 */
@Controller
@RequestMapping("/api/websocket")
public class WebSocketController {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketController.class);

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private CryptocurrencyService cryptocurrencyService;

    @Autowired
    private KrakenWebSocketService krakenWebSocketService;

    // ===============================================
    // WEBSOCKET STOMP MESSAGE HANDLERS
    // ===============================================

    /**
     * Handle subscription requests for price updates
     * Client sends: /app/subscribe-prices
     * Server responds to: /topic/prices
     */
    @MessageMapping("/subscribe-prices")
    @SendTo("/topic/prices")
    public Map<String, Object> subscribeToPrices() {
        try {
            logger.info("Client subscribed to price updates");

            // Send current prices immediately upon subscription
            Map<String, java.math.BigDecimal> currentPrices = cryptocurrencyService.getAllCurrentPrices();

            return Map.of(
                    "type", "initial_prices",
                    "data", currentPrices,
                    "timestamp", System.currentTimeMillis()
            );

        } catch (Exception e) {
            logger.error("Error handling price subscription", e);
            return Map.of(
                    "type", "error",
                    "message", "Failed to subscribe to prices",
                    "timestamp", System.currentTimeMillis()
            );
        }
    }

    /**
     * Handle connection status requests via STOMP
     * Client sends: /app/connection-status
     * Server responds to: /topic/status
     */
    @MessageMapping("/connection-status")
    @SendTo("/topic/status")
    public Map<String, Object> getConnectionStatusViaWebSocket() {
        try {
            KrakenWebSocketService.ConnectionStatus status = krakenWebSocketService.getConnectionStatus();

            return Map.of(
                    "type", "connection_status",
                    "connected", status.isConnected(),
                    "reconnectAttempts", status.getReconnectAttempts(),
                    "subscribedPairs", status.getSubscribedPairs(),
                    "lastHeartbeat", status.getLastHeartbeat(),
                    "timeSinceHeartbeat", status.getTimeSinceHeartbeat(),
                    "timestamp", System.currentTimeMillis()
            );

        } catch (Exception e) {
            logger.error("Error getting connection status", e);
            return Map.of(
                    "type", "error",
                    "message", "Failed to get connection status",
                    "timestamp", System.currentTimeMillis()
            );
        }
    }

    /**
     * Handle manual price refresh requests via STOMP
     * Client sends: /app/refresh-prices
     */
    @MessageMapping("/refresh-prices")
    public void refreshPricesViaWebSocket() {
        try {
            logger.info("Manual price refresh requested by client via WebSocket");

            // Trigger manual price update
            krakenWebSocketService.triggerPriceUpdate();

            // Send confirmation
            messagingTemplate.convertAndSend("/topic/status", Map.of(
                    "type", "refresh_complete",
                    "message", "Price refresh triggered",
                    "timestamp", System.currentTimeMillis()
            ));

        } catch (Exception e) {
            logger.error("Error refreshing prices", e);

            messagingTemplate.convertAndSend("/topic/status", Map.of(
                    "type", "error",
                    "message", "Failed to refresh prices: " + e.getMessage(),
                    "timestamp", System.currentTimeMillis()
            ));
        }
    }

    // ===============================================
    // REST API ENDPOINTS FOR MANAGEMENT
    // ===============================================

    /**
     * Get WebSocket connection status via REST API
     * GET /api/websocket/status
     */
    @GetMapping("/status")
    @ResponseBody
    public ResponseEntity<?> getConnectionStatus() {
        try {
            KrakenWebSocketService.ConnectionStatus status = krakenWebSocketService.getConnectionStatus();

            return ResponseEntity.ok(Map.of(
                    "connected", status.isConnected(),
                    "reconnectAttempts", status.getReconnectAttempts(),
                    "subscribedPairs", status.getSubscribedPairs(),
                    "lastHeartbeat", status.getLastHeartbeat(),
                    "timeSinceHeartbeat", status.getTimeSinceHeartbeat(),
                    "timestamp", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "error", "Failed to get connection status: " + e.getMessage()
            ));
        }
    }

    /**
     * Force WebSocket reconnection via REST API
     * POST /api/websocket/reconnect
     */
    @PostMapping("/reconnect")
    @ResponseBody
    public ResponseEntity<?> forceReconnect() {
        try {
            krakenWebSocketService.forceReconnect();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Reconnection initiated",
                    "timestamp", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "error", "Failed to initiate reconnection: " + e.getMessage()
            ));
        }
    }

    /**
     * Manually trigger price updates via REST API
     * POST /api/websocket/trigger-update
     */
    @PostMapping("/trigger-update")
    @ResponseBody
    public ResponseEntity<?> triggerPriceUpdate() {
        try {
            krakenWebSocketService.triggerPriceUpdate();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Price update triggered",
                    "timestamp", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "error", "Failed to trigger price update: " + e.getMessage()
            ));
        }
    }

    /**
     * Check if WebSocket is connected via REST API
     * GET /api/websocket/connected
     */
    @GetMapping("/connected")
    @ResponseBody
    public ResponseEntity<?> isConnected() {
        try {
            boolean connected = krakenWebSocketService.isConnected();

            return ResponseEntity.ok(Map.of(
                    "connected", connected,
                    "timestamp", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "connected", false,
                    "error", e.getMessage()
            ));
        }
    }

    // ===============================================
    // UTILITY METHODS
    // ===============================================

    /**
     * Send heartbeat to clients to keep connection alive
     */
    public void sendHeartbeat() {
        try {
            messagingTemplate.convertAndSend("/topic/heartbeat", Map.of(
                    "type", "heartbeat",
                    "timestamp", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            logger.error("Error sending heartbeat", e);
        }
    }
}