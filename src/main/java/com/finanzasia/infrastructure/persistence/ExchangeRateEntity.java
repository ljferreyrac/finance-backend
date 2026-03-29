package com.finanzasia.infrastructure.persistence;

import com.finanzasia.domain.model.ExchangeRate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "exchange_rates")
public class ExchangeRateEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "currency_from", nullable = false, length = 3, updatable = false)
    private String currencyFrom;

    @Column(name = "currency_to", nullable = false, length = 3, updatable = false)
    private String currencyTo;

    @Column(name = "buy_rate", nullable = false, precision = 10, scale = 4)
    private BigDecimal buyRate;

    @Column(name = "sell_rate", nullable = false, precision = 10, scale = 4)
    private BigDecimal sellRate;

    @Column(name = "rate_date", nullable = false, updatable = false)
    private LocalDate rateDate;

    @Column(name = "source", nullable = false, length = 20)
    private String source;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ExchangeRateEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getCurrencyFrom() { return currencyFrom; }
    public void setCurrencyFrom(String currencyFrom) { this.currencyFrom = currencyFrom; }

    public String getCurrencyTo() { return currencyTo; }
    public void setCurrencyTo(String currencyTo) { this.currencyTo = currencyTo; }

    public BigDecimal getBuyRate() { return buyRate; }
    public void setBuyRate(BigDecimal buyRate) { this.buyRate = buyRate; }

    public BigDecimal getSellRate() { return sellRate; }
    public void setSellRate(BigDecimal sellRate) { this.sellRate = sellRate; }

    public LocalDate getRateDate() { return rateDate; }
    public void setRateDate(LocalDate rateDate) { this.rateDate = rateDate; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public ExchangeRate toDomain() {
        return new ExchangeRate(
                id, currencyFrom, currencyTo,
                buyRate, sellRate, rateDate,
                source, createdAt, updatedAt);
    }

    public static ExchangeRateEntity fromDomain(ExchangeRate domain) {
        ExchangeRateEntity entity = new ExchangeRateEntity();
        entity.setId(domain.getId());
        entity.setCurrencyFrom(domain.getCurrencyFrom());
        entity.setCurrencyTo(domain.getCurrencyTo());
        entity.setBuyRate(domain.getBuyRate());
        entity.setSellRate(domain.getSellRate());
        entity.setRateDate(domain.getRateDate());
        entity.setSource(domain.getSource());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }
}
