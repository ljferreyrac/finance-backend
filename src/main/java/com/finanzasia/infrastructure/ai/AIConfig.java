package com.finanzasia.infrastructure.ai;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class AIConfig {

    /**
     * No custom timeouts configured; Cloud Run's platform request timeout (60s)
     * is relied upon as the outer bound.
     */
    @Bean
    public RestClient restClient() {
        return RestClient.create();
    }
}
