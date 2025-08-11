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
        scheduler.setPoolSize(16);
        scheduler.setThreadNamePrefix("websocket-instant-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(5);
        scheduler.initialize();
        return scheduler;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {

        config.enableSimpleBroker("/topic", "/queue")
                .setHeartbeatValue(new long[]{5000, 5000})
                .setTaskScheduler(messagingTaskScheduler());


        config.setApplicationDestinationPrefixes("/app");


        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {

        registry.addEndpoint(websocketEndpoint)
                .setAllowedOriginPatterns("*")
                .withSockJS()
                .setHeartbeatTime(15000)
                .setDisconnectDelay(2000)
                .setStreamBytesLimit(256 * 1024)
                .setHttpMessageCacheSize(2000)
                .setSessionCookieNeeded(false);


        registry.addEndpoint(websocketEndpoint)
                .setAllowedOriginPatterns("*");
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {

        registration.setMessageSizeLimit(128 * 1024)
                .setSendBufferSizeLimit(1024 * 1024)
                .setSendTimeLimit(10 * 1000)
                .setTimeToFirstMessage(15 * 1000);
    }
}