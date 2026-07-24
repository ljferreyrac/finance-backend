package com.finanzasia.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

// Named "Port" to distinguish it from the domain-facing JpaExchangeRateRepository adapter.
public interface JpaExchangeRateRepositoryPort extends JpaRepository<ExchangeRateEntity, UUID> {

    Optional<ExchangeRateEntity> findByCurrencyFromAndCurrencyToAndRateDate(
            String currencyFrom, String currencyTo, LocalDate rateDate);
}
