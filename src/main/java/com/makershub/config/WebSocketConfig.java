package com.makershub.config;

import com.makershub.security.WebSocketAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor webSocketAuthInterceptor;

    // H-10: Use same allowed origins as HTTP CORS policy
    @Value("#{'${makershub.cors.allowed-origins}'.split(',')}")
    private List<String> allowedOrigins;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable simple broker for topic destinations
        config.enableSimpleBroker("/topic");
        // Prefix for application-bound messages
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // H-10: Restrict to allowed origins instead of wildcard "*"
        String[] origins = allowedOrigins.toArray(new String[0]);
        registry.addEndpoint("/ws")
                .setAllowedOrigins(origins)
                .withSockJS();

        registry.addEndpoint("/ws")
                .setAllowedOrigins(origins);
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Intercept and authorize client connections and subscriptions
        registration.interceptors(webSocketAuthInterceptor);
    }
}
