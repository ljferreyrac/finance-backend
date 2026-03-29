package com.finanzasia.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * API response representation of a category.
 */
@Schema(description = "Category as returned by the API")
public record CategoryDTO(

        @Schema(description = "Unique category identifier", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID id,

        @Schema(description = "Category display name", example = "Comida")
        String name,

        @Schema(description = "Hex color code for UI display", example = "#FF5733")
        String color,

        @Schema(description = "Icon identifier for UI display", example = "food")
        String icon,

        @Schema(description = "Whether this is the user's default category")
        boolean isDefault,

        @Schema(description = "Display order position (lower numbers appear first)")
        int position,

        @Schema(description = "Number of non-deleted expenses referencing this category")
        long expenseCount
) {}
