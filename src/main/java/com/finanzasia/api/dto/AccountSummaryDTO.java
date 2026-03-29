package com.finanzasia.api.dto;

import com.finanzasia.domain.model.AccountType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * Compact account reference embedded inside a transaction response.
 */
@Schema(description = "Compact account reference embedded inside a transaction response")
public record AccountSummaryDTO(

        @Schema(description = "Account UUID") UUID id,
        @Schema(description = "Display name", example = "BCP Soles") String name,
        @Schema(description = "Hex color for UI", example = "#0066CC") String color,
        @Schema(description = "Account type") AccountType type
) {}
