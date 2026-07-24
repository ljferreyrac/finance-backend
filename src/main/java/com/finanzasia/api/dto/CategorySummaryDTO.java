package com.finanzasia.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Compact category reference embedded inside an expense response")
public record CategorySummaryDTO(

        @Schema(description = "Category UUID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID id,

        @Schema(description = "Category display name", example = "Comida")
        String name,

        @Schema(description = "Hex color code for UI display", example = "#FF5733")
        String color,

        @Schema(description = "Icon identifier for UI display", example = "food")
        String icon
) {}
