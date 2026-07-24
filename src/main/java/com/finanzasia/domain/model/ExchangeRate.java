package com.finanzasia.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * The sell rate is used for expense conversion because that is the price the
 * user pays when the bank converts their soles to dollars (or vice versa).
 * No JPA or framework annotations are allowed here.
 */
public final class ExchangeRate {

    private final UUID id;
    private final String currencyFrom;
    private final String currencyTo;
    private BigDecimal buyRate;
    private BigDecimal sellRate;
    private final LocalDate rateDate;
    private String source;
    private final Instant createdAt;
    private Instant updatedAt;

    public ExchangeRate(
            UUID id,
            String currencyFrom,
            String currencyTo,
            BigDecimal buyRate,
            BigDecimal sellRate,
            LocalDate rateDate,
            String source,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.currencyFrom = currencyFrom;
        this.currencyTo = currencyTo;
        this.buyRate = buyRate;
        this.sellRate = sellRate;
        this.rateDate = rateDate;
        this.source = source;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public String getCurrencyFrom() { return currencyFrom; }
    public String getCurrencyTo() { return currencyTo; }
    public BigDecimal getBuyRate() { return buyRate; }
    public BigDecimal getSellRate() { return sellRate; }
    public LocalDate getRateDate() { return rateDate; }
    public String getSource() { return source; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setBuyRate(BigDecimal buyRate) { this.buyRate = buyRate; }
    public void setSellRate(BigDecimal sellRate) { this.sellRate = sellRate; }
    public void setSource(String source) { this.source = source; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    /**
     * @param amount a positive value in the source currency
     * @return the equivalent amount in the target currency, rounded to 2 decimal places
     */
    public BigDecimal toAccountCurrency(BigDecimal amount) {
        return amount.multiply(sellRate).setScale(2, RoundingMode.HALF_UP);
    }
}
