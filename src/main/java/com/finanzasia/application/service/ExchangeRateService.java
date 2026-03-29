package com.finanzasia.application.service;

import com.finanzasia.domain.model.ExchangeRate;
import com.finanzasia.domain.port.in.GetTodayExchangeRateUseCase;
import com.finanzasia.domain.port.in.UpsertExchangeRateUseCase;
import com.finanzasia.domain.port.out.ExchangeRateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Orchestrates exchange rate use cases.
 * When no rate is recorded for today, a conservative default (buy=3.69,
 * sell=3.74) is persisted so the rest of the application always has a
 * usable rate without requiring explicit setup.
 */
@Service
public class ExchangeRateService implements GetTodayExchangeRateUseCase, UpsertExchangeRateUseCase {

    static final BigDecimal DEFAULT_BUY_RATE  = new BigDecimal("3.69");
    static final BigDecimal DEFAULT_SELL_RATE = new BigDecimal("3.74");
    static final String     DEFAULT_SOURCE    = "MANUAL";

    private static final String CURRENCY_FROM = "USD";
    private static final String CURRENCY_TO   = "PEN";

    private final ExchangeRateRepository exchangeRateRepository;

    public ExchangeRateService(ExchangeRateRepository exchangeRateRepository) {
        this.exchangeRateRepository = exchangeRateRepository;
    }

    @Override
    @Transactional
    public ExchangeRate getOrCreateDefault() {
        LocalDate today = LocalDate.now();
        return exchangeRateRepository.findByDate(CURRENCY_FROM, CURRENCY_TO, today)
                .orElseGet(() -> {
                    Instant now = Instant.now();
                    ExchangeRate defaultRate = new ExchangeRate(
                            UUID.randomUUID(),
                            CURRENCY_FROM,
                            CURRENCY_TO,
                            DEFAULT_BUY_RATE,
                            DEFAULT_SELL_RATE,
                            today,
                            DEFAULT_SOURCE,
                            now,
                            now);
                    return exchangeRateRepository.upsert(defaultRate);
                });
    }

    @Override
    @Transactional
    public ExchangeRate upsertToday(BigDecimal buyRate, BigDecimal sellRate) {
        LocalDate today = LocalDate.now();
        Instant now = Instant.now();

        return exchangeRateRepository.findByDate(CURRENCY_FROM, CURRENCY_TO, today)
                .map(existing -> {
                    existing.setBuyRate(buyRate);
                    existing.setSellRate(sellRate);
                    existing.setSource(DEFAULT_SOURCE);
                    existing.setUpdatedAt(now);
                    return exchangeRateRepository.upsert(existing);
                })
                .orElseGet(() -> {
                    ExchangeRate newRate = new ExchangeRate(
                            UUID.randomUUID(),
                            CURRENCY_FROM,
                            CURRENCY_TO,
                            buyRate,
                            sellRate,
                            today,
                            DEFAULT_SOURCE,
                            now,
                            now);
                    return exchangeRateRepository.upsert(newRate);
                });
    }
}
