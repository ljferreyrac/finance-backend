package com.finanzasia.domain.port.in;

import com.finanzasia.domain.model.ExchangeRate;

import java.math.BigDecimal;

public interface UpsertExchangeRateUseCase {

    /**
     * @param buyRate  the bank's buy rate (must be positive)
     * @param sellRate the bank's sell rate (must be positive and >= buyRate)
     */
    ExchangeRate upsertToday(BigDecimal buyRate, BigDecimal sellRate);
}
