package com.finanzasia.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ExchangeRateTest {

    private ExchangeRate buildRate() {
        Instant now = Instant.now();
        return new ExchangeRate(UUID.randomUUID(), "USD", "PEN",
                new BigDecimal("3.69"), new BigDecimal("3.74"), LocalDate.now(), "MANUAL", now, now);
    }

    @Test
    @DisplayName("getters return the values passed to the constructor")
    void gettersReturnConstructorValues() {
        UUID id = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 3, 20);
        Instant createdAt = Instant.now().minusSeconds(60);
        Instant updatedAt = Instant.now();

        ExchangeRate rate = new ExchangeRate(id, "USD", "PEN",
                new BigDecimal("3.70"), new BigDecimal("3.75"), date, "MANUAL", createdAt, updatedAt);

        assertThat(rate.getId()).isEqualTo(id);
        assertThat(rate.getCurrencyFrom()).isEqualTo("USD");
        assertThat(rate.getCurrencyTo()).isEqualTo("PEN");
        assertThat(rate.getBuyRate()).isEqualByComparingTo("3.70");
        assertThat(rate.getSellRate()).isEqualByComparingTo("3.75");
        assertThat(rate.getRateDate()).isEqualTo(date);
        assertThat(rate.getSource()).isEqualTo("MANUAL");
        assertThat(rate.getCreatedAt()).isEqualTo(createdAt);
        assertThat(rate.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    @DisplayName("setters update their corresponding getters")
    void settersUpdateState() {
        ExchangeRate rate = buildRate();
        Instant newUpdatedAt = Instant.now();

        rate.setBuyRate(new BigDecimal("3.71"));
        rate.setSellRate(new BigDecimal("3.76"));
        rate.setSource("BCRP");
        rate.setUpdatedAt(newUpdatedAt);

        assertThat(rate.getBuyRate()).isEqualByComparingTo("3.71");
        assertThat(rate.getSellRate()).isEqualByComparingTo("3.76");
        assertThat(rate.getSource()).isEqualTo("BCRP");
        assertThat(rate.getUpdatedAt()).isEqualTo(newUpdatedAt);
    }

    @Test
    @DisplayName("toAccountCurrency multiplies by the sell rate and rounds half-up to 2 decimals")
    void toAccountCurrencyUsesSellRate() {
        ExchangeRate rate = buildRate();
        rate.setSellRate(new BigDecimal("3.345"));

        // 1.00 * 3.345 = 3.345: the digit before the rounding point is 4 (even), so HALF_EVEN
        // would round down to 3.34, but HALF_UP must round to 3.35.
        BigDecimal result = rate.toAccountCurrency(new BigDecimal("1.00"));

        assertThat(result).isEqualByComparingTo("3.35");
    }
}
