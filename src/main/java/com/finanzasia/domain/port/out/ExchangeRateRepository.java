package com.finanzasia.domain.port.out;

import com.finanzasia.domain.model.ExchangeRate;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Must remain free of any JPA, Spring, or JDBC types.
 */
public interface ExchangeRateRepository {

    Optional<ExchangeRate> findByDate(String currencyFrom, String currencyTo, LocalDate date);

    /** Keyed by currency pair and date: inserts if absent, otherwise updates. */
    ExchangeRate upsert(ExchangeRate rate);
}
