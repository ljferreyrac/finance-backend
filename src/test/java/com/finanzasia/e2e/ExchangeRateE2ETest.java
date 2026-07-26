package com.finanzasia.e2e;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code /api/v1/exchange-rates/today} end to end.
 *
 * <p>Worth running against a real database specifically because the rate for today already
 * exists before any test touches it: {@code V1__initial_schema.sql} seeds
 * {@code ('USD','PEN',3.69,3.74,CURRENT_DATE,'MANUAL')}. So this exercises the
 * "row already present" branch of {@code ExchangeRateService.getOrCreateDefault}, and
 * confirms the seed itself lands correctly through Flyway.
 */
class ExchangeRateE2ETest extends AbstractE2ETest {

    @Test
    @DisplayName("returns today's USD-to-PEN rate, matching the values seeded by the migration")
    void returnsTodaysSeededRate() {
        TokenPair user = registerAndLogin("fx-today");

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/exchange-rates/today", HttpMethod.GET, authorized(user.accessToken()), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = response.getBody();
        assertThat(body.get("currencyFrom")).isEqualTo("USD");
        assertThat(body.get("currencyTo")).isEqualTo("PEN");
        assertThat(new BigDecimal(body.get("buyRate").toString())).isEqualByComparingTo("3.69");
        assertThat(new BigDecimal(body.get("sellRate").toString())).isEqualByComparingTo("3.74");
        assertThat(body.get("rateDate")).isEqualTo(LocalDate.now().toString());
        assertThat(body.get("source")).isEqualTo("MANUAL");
    }

    @Test
    @DisplayName("is idempotent: repeated calls never violate the pair+date unique constraint")
    void repeatedCallsAreIdempotent() {
        TokenPair user = registerAndLogin("fx-idempotent");

        ResponseEntity<Map> first = restTemplate.exchange(
                "/api/v1/exchange-rates/today", HttpMethod.GET, authorized(user.accessToken()), Map.class);
        ResponseEntity<Map> second = restTemplate.exchange(
                "/api/v1/exchange-rates/today", HttpMethod.GET, authorized(user.accessToken()), Map.class);

        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(second.getBody()).isEqualTo(first.getBody());
    }

    @Test
    @DisplayName("the rate is system-global: a different user sees the same row")
    void rateIsGlobalNotPerUser() {
        TokenPair userA = registerAndLogin("fx-user-a");
        TokenPair userB = registerAndLogin("fx-user-b");

        ResponseEntity<Map> forA = restTemplate.exchange(
                "/api/v1/exchange-rates/today", HttpMethod.GET, authorized(userA.accessToken()), Map.class);
        ResponseEntity<Map> forB = restTemplate.exchange(
                "/api/v1/exchange-rates/today", HttpMethod.GET, authorized(userB.accessToken()), Map.class);

        assertThat(forB.getBody()).isEqualTo(forA.getBody());
    }

    @Test
    @DisplayName("requires authentication")
    void requiresAuthentication() {
        ResponseEntity<Map> response = restTemplate.getForEntity("/api/v1/exchange-rates/today", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
