package com.finanzasia.domain.port.in;

import com.finanzasia.domain.model.ExchangeRate;

import java.math.BigDecimal;

/**
 * Input port for manually updating today's USD-to-PEN exchange rate.
 */
public interface UpsertExchangeRateUseCase {

    /**
     * Creates or updates today's USD-to-PEN exchange rate record using the
     * provided buy and sell rates.
     *
     * @param buyRate  the bank's buy rate (must be positive)
     * @param sellRate the bank's sell rate (must be positive and >= buyRate)
     * @return the persisted exchange rate
     */
    ExchangeRate upsertToday(BigDecimal buyRate, BigDecimal sellRate);
}
