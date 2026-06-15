package com.itaccess.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.socket.config.annotation.*;

/**
 * Configuration pour l'activation des WebSockets avec STOMP.
 * Permet la communication bidirectionnelle en temps réel entre le serveur et les clients.
 */
@Configuration
@EnableWebSocketMessageBroker // Active la gestion des messages WebSocket via un broker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${app.cors.allowed-origins:*}")
    private String allowedOrigins;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue"); // Active un broker simple en mémoire pour envoyer des messages aux clients
        config.setApplicationDestinationPrefixes("/app"); // Préfixe pour les messages envoyés du client vers le serveur
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        String[] origins = allowedOrigins.split(",");
        registry.addEndpoint("/ws").setAllowedOriginPatterns(origins).withSockJS(); // Point d'entrée pour la connexion WebSocket (utilisé par SockJS côté React)
    }
}