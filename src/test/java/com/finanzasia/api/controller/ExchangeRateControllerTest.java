package com.finanzasia.api.controller;

import com.finanzasia.api.security.UserPrincipal;
import com.finanzasia.domain.model.ExchangeRate;
import com.finanzasia.domain.port.in.AuthenticateAccessTokenUseCase;
import com.finanzasia.domain.port.in.GetTodayExchangeRateUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExchangeRateController.class)
class ExchangeRateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetTodayExchangeRateUseCase getTodayExchangeRateUseCase;

    @MockitoBean
    private AuthenticateAccessTokenUseCase authenticateAccessToken;

    @MockitoBean
    private StringRedisTemplate redisTemplate;

    private final UserPrincipal principal = new UserPrincipal(UUID.randomUUID(), "user@example.com");

    @Test
    @DisplayName("GET /api/v1/exchange-rates/today returns the current rate as JSON")
    void getTodayReturnsCurrentRate() throws Exception {
        Instant now = Instant.now();
        ExchangeRate rate = new ExchangeRate(UUID.randomUUID(), "USD", "PEN",
                new BigDecimal("3.69"), new BigDecimal("3.74"), LocalDate.now(), "MANUAL", now, now);
        when(getTodayExchangeRateUseCase.getOrCreateDefault()).thenReturn(rate);

        mockMvc.perform(get("/api/v1/exchange-rates/today").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currencyFrom").value("USD"))
                .andExpect(jsonPath("$.currencyTo").value("PEN"))
                .andExpect(jsonPath("$.buyRate").value(3.69))
                .andExpect(jsonPath("$.sellRate").value(3.74))
                .andExpect(jsonPath("$.source").value("MANUAL"));
    }

    @Test
    @DisplayName("GET /api/v1/exchange-rates/today without authentication is rejected with 401")
    void getTodayWithoutAuthIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/exchange-rates/today"))
                .andExpect(status().isUnauthorized());
    }
}
