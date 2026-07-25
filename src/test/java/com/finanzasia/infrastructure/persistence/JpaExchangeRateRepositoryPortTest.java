package com.finanzasia.infrastructure.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class JpaExchangeRateRepositoryPortTest extends AbstractPostgresTest {

    @Autowired
    private JpaExchangeRateRepositoryPort exchangeRatePort;

    @Autowired
    private TestEntityManager em;

    @Test
    @DisplayName("finds the exact currency pair and date, and nothing else")
    void findsExactMatchOnly() {
        PersistenceFixtures.exchangeRate(em, "USD", "PEN",
                new BigDecimal("3.7000"), new BigDecimal("3.8000"), LocalDate.of(2026, 3, 15));
        PersistenceFixtures.exchangeRate(em, "USD", "PEN",
                new BigDecimal("3.7100"), new BigDecimal("3.8100"), LocalDate.of(2026, 3, 16));
        PersistenceFixtures.exchangeRate(em, "PEN", "USD",
                new BigDecimal("0.2700"), new BigDecimal("0.2800"), LocalDate.of(2026, 3, 15));

        Optional<ExchangeRateEntity> found = exchangeRatePort.findByCurrencyFromAndCurrencyToAndRateDate(
                "USD", "PEN", LocalDate.of(2026, 3, 15));

        assertThat(found).isPresent();
        assertThat(found.get().getBuyRate()).isEqualByComparingTo("3.7000");
    }

    @Test
    @DisplayName("returns empty when no rate exists for the given pair and date")
    void emptyWhenNoMatch() {
        Optional<ExchangeRateEntity> found = exchangeRatePort.findByCurrencyFromAndCurrencyToAndRateDate(
                "USD", "PEN", LocalDate.of(2026, 3, 15));

        assertThat(found).isEmpty();
    }
}
