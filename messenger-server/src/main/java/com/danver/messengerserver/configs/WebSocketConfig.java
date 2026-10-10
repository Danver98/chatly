package com.danver.messengerserver.configs;

import com.danver.messengerserver.interceptors.SubscriptionInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import com.danver.messengerserver.utils.Constants;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private SubscriptionInterceptor subscriptionInterceptor;

    @Autowired
    public WebSocketConfig(SubscriptionInterceptor subscriptionInterceptor) {
        this.subscriptionInterceptor = subscriptionInterceptor;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        //registry.addEndpoint(Constants.WS_MESSAGE_SERVICE_STOMP_ENDPOINT);
        registry.addEndpoint(Constants.MESSAGE_BROKER_DESTINATION_PREFIX)
                .setAllowedOrigins("*")
                .withSockJS();
        registry.addEndpoint(Constants.MESSAGE_BROKER_DESTINATION_PREFIX)
                .setAllowedOrigins("*");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        //registry.enableSimpleBroker(Constants.MESSAGE_BROKER_TOPIC_PREFIX, Constants.MESSAGE_BROKER_QUEUE_PREFIX);
        registry.enableSimpleBroker(Constants.MESSAGE_BROKER_TOPIC_PREFIX, Constants.MESSAGE_BROKER_USER_DESTINATION_PREFIX);
        registry.setApplicationDestinationPrefixes(Constants.MESSAGE_BROKER_APPLICATION_DESTINATION_PREFIX);
        registry.setUserDestinationPrefix(Constants.MESSAGE_BROKER_USER_DESTINATION_PREFIX);
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Register interceptor for STOMP subscription authorization
        registration.interceptors(subscriptionInterceptor);
    }
}
