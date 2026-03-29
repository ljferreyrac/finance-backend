package com.finanzasia.api.controller;

import com.finanzasia.api.dto.ExchangeRateDTO;
import com.finanzasia.domain.model.ExchangeRate;
import com.finanzasia.domain.port.in.GetTodayExchangeRateUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the daily USD-to-PEN exchange rate for read access.
 * The rate is system-global; it is seeded automatically on first access and
 * is not editable via this API. Users confirm their own PEN equivalent by
 * supplying {@code amountLocal} on a cross-currency transaction instead.
 */
@Tag(name = "Exchange Rates", description = "Daily USD-to-PEN exchange rate (read-only)")
@RestController
@RequestMapping("/api/v1/exchange-rates")
@Validated
public class ExchangeRateController {

    private final GetTodayExchangeRateUseCase getTodayExchangeRateUseCase;

    public ExchangeRateController(GetTodayExchangeRateUseCase getTodayExchangeRateUseCase) {
        this.getTodayExchangeRateUseCase = getTodayExchangeRateUseCase;
    }

    @Operation(summary = "Get today's exchange rate",
               description = "Returns the USD-to-PEN exchange rate for today. "
                           + "If none has been recorded yet, a default MANUAL rate "
                           + "(buy=3.69, sell=3.74) is created and returned.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Today's exchange rate",
                         content = @Content(schema = @Schema(implementation = ExchangeRateDTO.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content)
    })
    @GetMapping("/today")
    public ExchangeRateDTO getToday() {
        return toDTO(getTodayExchangeRateUseCase.getOrCreateDefault());
    }

    // --- presentation helpers ---

    private ExchangeRateDTO toDTO(ExchangeRate rate) {
        return new ExchangeRateDTO(
                rate.getCurrencyFrom(),
                rate.getCurrencyTo(),
                rate.getBuyRate(),
                rate.getSellRate(),
                rate.getRateDate(),
                rate.getSource());
    }
}
