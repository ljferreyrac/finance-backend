package com.finanzasia.domain.port.in;

import com.finanzasia.domain.model.ExchangeRate;

/**
 * Input port for retrieving today's USD-to-PEN exchange rate.
 * If no rate has been recorded for today, a default MANUAL rate is
 * persisted and returned so callers never receive an empty result.
 */
public interface GetTodayExchangeRateUseCase {

    /**
     * Returns today's USD-to-PEN exchange rate.
     * If none exists a default entry (buy=3.69, sell=3.74, source=MANUAL) is
     * created, persisted, and returned.
     */
    ExchangeRate getOrCreateDefault();
}
