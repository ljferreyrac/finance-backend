package com.finanzasia.infrastructure.persistence;

import com.finanzasia.domain.model.ExchangeRate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaExchangeRateRepositoryTest {

    @Mock
    private JpaExchangeRateRepositoryPort jpaPort;

    private JpaExchangeRateRepository repository;

    @BeforeEach
    void setUp() {
        repository = new JpaExchangeRateRepository(jpaPort);
    }

    private ExchangeRate buildRate() {
        Instant now = Instant.now();
        return new ExchangeRate(UUID.randomUUID(), "USD", "PEN",
                new BigDecimal("3.69"), new BigDecimal("3.74"), LocalDate.now(), "MANUAL", now, now);
    }

    @Test
    @DisplayName("findByDate maps a present entity")
    void findByDateMapsPresentEntity() {
        ExchangeRate rate = buildRate();
        ExchangeRateEntity entity = ExchangeRateEntity.fromDomain(rate);
        when(jpaPort.findByCurrencyFromAndCurrencyToAndRateDate("USD", "PEN", rate.getRateDate()))
                .thenReturn(Optional.of(entity));

        Optional<ExchangeRate> result = repository.findByDate("USD", "PEN", rate.getRateDate());

        assertThat(result).isPresent();
        assertThat(result.get().getBuyRate()).isEqualByComparingTo(rate.getBuyRate());
    }

    @Test
    @DisplayName("findByDate returns empty when the port finds nothing")
    void findByDateReturnsEmptyWhenAbsent() {
        when(jpaPort.findByCurrencyFromAndCurrencyToAndRateDate("USD", "PEN", LocalDate.now()))
                .thenReturn(Optional.empty());

        assertThat(repository.findByDate("USD", "PEN", LocalDate.now())).isEmpty();
    }

    @Test
    @DisplayName("upsert converts the domain object to an entity and back")
    void upsertConvertsToEntityAndBack() {
        ExchangeRate rate = buildRate();
        ExchangeRateEntity savedEntity = ExchangeRateEntity.fromDomain(rate);
        when(jpaPort.save(any(ExchangeRateEntity.class))).thenReturn(savedEntity);

        ExchangeRate result = repository.upsert(rate);

        assertThat(result.getId()).isEqualTo(rate.getId());
        assertThat(result.getSellRate()).isEqualByComparingTo(rate.getSellRate());
    }
}
