package com.finanzasia.infrastructure.ai;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Registers shared infrastructure beans for AI HTTP integrations.
 */
@Configuration
public class AIConfig {

    /**
     * A default {@link RestClient} instance shared by all AI integration services.
     * Timeouts and retry behaviour are delegated to the default JDK HTTP client
     * for simplicity; production deployments on Cloud Run rely on the platform's
     * request timeout (60 s) as the outer bound.
     */
    @Bean
    public RestClient restClient() {
        return RestClient.create();
    }
}
