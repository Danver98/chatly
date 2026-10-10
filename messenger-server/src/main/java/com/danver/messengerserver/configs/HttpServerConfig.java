package com.danver.messengerserver.configs;

import org.apache.catalina.connector.Connector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HttpServerConfig {
    /**
     * Additionally turn on plain http connections
     */
    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> cookieProcessorCustomizer() {
        return (TomcatServletWebServerFactory factory) -> {
            // also listen on http
            final Connector connector = new Connector();
            connector.setPort(httpPort);
            factory.addAdditionalConnectors(connector);
        };
    }

    @Value("${server.http.port}")
    private int httpPort;
}

