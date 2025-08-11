package com.cryptotrading.crypto_trading_sim.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

/**
 * ⚡ МАКСИМАЛНО ОПТИМИЗИРАНА WebSocket configuration за INSTANT real-time price updates
 * 🚀 ZERO DELAYS - Configured for professional-grade максимална скорост
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${websocket.endpoint:/ws}")
    private String websocketEndpoint;

    @Value("${websocket.topic.prices:/topic/prices}")
    private String pricesTopicPrefix;

    /**
     * ⚡ Create HIGH-PERFORMANCE TaskScheduler bean за INSTANT WebSocket heartbeat functionality
     */
    @Bean
    public ThreadPoolTaskScheduler messagingTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(16); // ⚡ УВЕЛИЧЕНО от 8 на 16 за максимална скорост
        scheduler.setThreadNamePrefix("websocket-instant-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(5); // ⚡ НАМАЛЕНО за по-бързо shutdown
        scheduler.initialize();
        return scheduler;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // ⚡ Enable INSTANT broker с максимално оптимизирани settings
        config.enableSimpleBroker("/topic", "/queue")
                .setHeartbeatValue(new long[]{5000, 5000}) // ⚡ НАМАЛЕНО от 10 на 5 секунди за по-бързо heartbeat
                .setTaskScheduler(messagingTaskScheduler()); // ⚡ HIGH-PERFORMANCE TaskScheduler

        // Set application destination prefix
        config.setApplicationDestinationPrefixes("/app");

        // Configure user destination prefix за user-specific messages
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // ⚡ Register the "/ws" endpoint за INSTANT WebSocket connections с максимално оптимизирани settings
        registry.addEndpoint(websocketEndpoint)
                .setAllowedOriginPatterns("*") // For development - restrict in production
                .withSockJS()
                .setHeartbeatTime(15000) // ⚡ НАМАЛЕНО от 25 на 15 секунди за по-бързо SockJS heartbeat
                .setDisconnectDelay(2000) // ⚡ НАМАЛЕНО от 5 на 2 секунди disconnect delay
                .setStreamBytesLimit(256 * 1024) // ⚡ УВЕЛИЧЕНО от 128KB на 256KB stream limit за optimal performance
                .setHttpMessageCacheSize(2000) // ⚡ УВЕЛИЧЕНО от 1000 на 2000 cache size за HTTP polling
                .setSessionCookieNeeded(false); // Disable session cookies за better performance

        // ⚡ ALSO register without SockJS за PURE INSTANT WebSocket connections (максимална performance)
        registry.addEndpoint(websocketEndpoint)
                .setAllowedOriginPatterns("*");
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        // ⚡ МАКСИМАЛНО optimize WebSocket transport за INSTANT real-time performance
        registration.setMessageSizeLimit(128 * 1024) // ⚡ УВЕЛИЧЕНО от 64KB на 128KB message size limit
                .setSendBufferSizeLimit(1024 * 1024) // ⚡ УВЕЛИЧЕНО от 512KB на 1MB send buffer
                .setSendTimeLimit(10 * 1000) // ⚡ НАМАЛЕНО от 20 на 10 секунди send timeout за по-бързо processing
                .setTimeToFirstMessage(15 * 1000); // ⚡ НАМАЛЕНО от 30 на 15 секунди time to first message
    }
}