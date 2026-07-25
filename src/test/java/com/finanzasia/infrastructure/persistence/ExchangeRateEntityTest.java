package com.finanzasia.infrastructure.persistence;

import com.finanzasia.domain.model.ExchangeRate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ExchangeRateEntityTest {

    @Test
    @DisplayName("fromDomain followed by toDomain round-trips every field")
    void roundTripsAllFields() {
        Instant now = Instant.now();
        ExchangeRate rate = new ExchangeRate(UUID.randomUUID(), "USD", "PEN",
                new BigDecimal("3.69"), new BigDecimal("3.74"), LocalDate.now(), "MANUAL", now, now);

        ExchangeRateEntity entity = ExchangeRateEntity.fromDomain(rate);
        ExchangeRate roundTripped = entity.toDomain();

        assertThat(roundTripped.getId()).isEqualTo(rate.getId());
        assertThat(roundTripped.getCurrencyFrom()).isEqualTo(rate.getCurrencyFrom());
        assertThat(roundTripped.getCurrencyTo()).isEqualTo(rate.getCurrencyTo());
        assertThat(roundTripped.getBuyRate()).isEqualByComparingTo(rate.getBuyRate());
        assertThat(roundTripped.getSellRate()).isEqualByComparingTo(rate.getSellRate());
        assertThat(roundTripped.getRateDate()).isEqualTo(rate.getRateDate());
        assertThat(roundTripped.getSource()).isEqualTo(rate.getSource());
        assertThat(roundTripped.getCreatedAt()).isEqualTo(rate.getCreatedAt());
        assertThat(roundTripped.getUpdatedAt()).isEqualTo(rate.getUpdatedAt());
    }

    @Test
    @DisplayName("every getter reflects the value passed to its setter")
    void gettersReflectSetterValues() {
        UUID id = UUID.randomUUID();
        LocalDate rateDate = LocalDate.of(2026, 3, 20);
        Instant createdAt = Instant.now().minusSeconds(60);
        Instant updatedAt = Instant.now();

        ExchangeRateEntity entity = new ExchangeRateEntity();
        entity.setId(id);
        entity.setCurrencyFrom("USD");
        entity.setCurrencyTo("PEN");
        entity.setBuyRate(new BigDecimal("3.70"));
        entity.setSellRate(new BigDecimal("3.75"));
        entity.setRateDate(rateDate);
        entity.setSource("MANUAL");
        entity.setCreatedAt(createdAt);
        entity.setUpdatedAt(updatedAt);

        assertThat(entity.getId()).isEqualTo(id);
        assertThat(entity.getCurrencyFrom()).isEqualTo("USD");
        assertThat(entity.getCurrencyTo()).isEqualTo("PEN");
        assertThat(entity.getBuyRate()).isEqualByComparingTo("3.70");
        assertThat(entity.getSellRate()).isEqualByComparingTo("3.75");
        assertThat(entity.getRateDate()).isEqualTo(rateDate);
        assertThat(entity.getSource()).isEqualTo("MANUAL");
        assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
        assertThat(entity.getUpdatedAt()).isEqualTo(updatedAt);
    }
}
