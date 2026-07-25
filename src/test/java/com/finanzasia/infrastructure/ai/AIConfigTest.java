package com.finanzasia.infrastructure.ai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

class AIConfigTest {

    @Test
    @DisplayName("restClient builds a usable RestClient instance")
    void restClientBeanBuildsInstance() {
        assertThat(new AIConfig().restClient()).isInstanceOf(RestClient.class);
    }
}
