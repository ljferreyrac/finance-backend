package com.finanzasia.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * API response representation of a daily exchange rate.
 */
@Schema(description = "Daily exchange rate for a currency pair")
public record ExchangeRateDTO(

        @Schema(description = "Source currency ISO code", example = "USD")
        String currencyFrom,

        @Schema(description = "Target currency ISO code", example = "PEN")
        String currencyTo,

        @Schema(description = "Bank buy rate: what the bank pays when the user sells foreign currency",
                example = "3.69")
        BigDecimal buyRate,

        @Schema(description = "Bank sell rate: what the user pays when buying foreign currency",
                example = "3.74")
        BigDecimal sellRate,

        @Schema(description = "The calendar date this rate applies to", example = "2026-03-23")
        LocalDate rateDate,

        @Schema(description = "Origin of the rate: MANUAL or an external provider", example = "MANUAL")
        String source
) {}
