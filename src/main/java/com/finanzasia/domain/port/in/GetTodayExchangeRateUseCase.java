package com.finanzasia.domain.port.in;

import com.finanzasia.domain.model.ExchangeRate;

public interface GetTodayExchangeRateUseCase {

    /**
     * If no rate exists for today, a default entry (buy=3.69, sell=3.74,
     * source=MANUAL) is created, persisted, and returned instead of an empty result.
     */
    ExchangeRate getOrCreateDefault();
}
