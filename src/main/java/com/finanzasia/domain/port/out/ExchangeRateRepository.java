package com.finanzasia.domain.port.out;

import com.finanzasia.domain.model.ExchangeRate;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Output port for exchange rate persistence.
 * Implementations live in the infrastructure layer; this interface must
 * remain free of any JPA, Spring, or JDBC types.
 */
public interface ExchangeRateRepository {

    /**
     * Returns the exchange rate for the given currency pair on the given date,
     * or empty if no record exists yet.
     */
    Optional<ExchangeRate> findByDate(String currencyFrom, String currencyTo, LocalDate date);

    /**
     * Inserts a new exchange rate record or updates the existing one for the
     * same currency pair and date. Returns the persisted state.
     */
    ExchangeRate upsert(ExchangeRate rate);
}
