package com.finanzasia.infrastructure.persistence;

import com.finanzasia.domain.model.ExchangeRate;
import com.finanzasia.domain.port.out.ExchangeRateRepository;

import java.time.LocalDate;
import java.util.Optional;

public class JpaExchangeRateRepository implements ExchangeRateRepository {

    private final JpaExchangeRateRepositoryPort jpaPort;

    public JpaExchangeRateRepository(JpaExchangeRateRepositoryPort jpaPort) {
        this.jpaPort = jpaPort;
    }

    @Override
    public Optional<ExchangeRate> findByDate(String currencyFrom, String currencyTo, LocalDate date) {
        return jpaPort.findByCurrencyFromAndCurrencyToAndRateDate(currencyFrom, currencyTo, date)
                .map(ExchangeRateEntity::toDomain);
    }

    @Override
    public ExchangeRate upsert(ExchangeRate rate) {
        ExchangeRateEntity entity = ExchangeRateEntity.fromDomain(rate);
        return jpaPort.save(entity).toDomain();
    }
}
