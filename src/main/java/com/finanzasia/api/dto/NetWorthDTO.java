package com.finanzasia.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Snapshot of the user's net worth across all active accounts")
public record NetWorthDTO(

        @Schema(description = "Sum of balances in PEN accounts", example = "5000.00") BigDecimal totalPEN,
        @Schema(description = "Sum of balances in USD accounts", example = "200.00") BigDecimal totalUSD,
        @Schema(description = "All accounts belonging to the user") List<AccountDTO> accounts
) {}
