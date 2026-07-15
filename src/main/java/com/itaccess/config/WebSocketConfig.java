package com.itaccess.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.socket.config.annotation.*;
import com.itaccess.security.WebSocketAuthInterceptor;
import lombok.RequiredArgsConstructor;

/**
 * Configuration pour l'activation des WebSockets avec STOMP.
 * Permet la communication bidirectionnelle en temps réel entre le serveur et les clients.
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${app.cors.allowed-origins:*}")
    private String allowedOrigins;

    private final WebSocketAuthInterceptor webSocketAuthInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(webSocketAuthInterceptor);
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        String[] origins = allowedOrigins.split(",");
        registry.addEndpoint("/ws").setAllowedOriginPatterns(origins).withSockJS();
    }
}
