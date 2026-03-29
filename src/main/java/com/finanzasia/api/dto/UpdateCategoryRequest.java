package com.finanzasia.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request body for PUT /api/v1/categories/{id}.
 * All fields are optional; only provided fields are updated.
 * Color and icon are nullable (setting them to null clears the existing value).
 */
@Schema(description = "Payload for updating an existing category")
public record UpdateCategoryRequest(

        @Schema(description = "Category display name", example = "Gastos de casa")
        @Size(max = 100, message = "name must not exceed 100 characters")
        String name,

        @Schema(description = "Hex color code for UI display. Null clears the existing color.",
                example = "#3498DB")
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$",
                 message = "color must be a valid 6-digit hex color code (e.g. #3498DB)")
        String color,

        @Schema(description = "Icon identifier for UI display. Null clears the existing icon.",
                example = "🏠")
        @Pattern(regexp = "^[\\p{L}\\p{N}\\p{So}\\p{Sk}\\-_]{1,50}$",
                 message = "icon must be a single emoji or a word of up to 50 characters")
        String icon,

        @Schema(description = "Display order position (lower numbers appear first)", example = "3")
        @Min(value = 0, message = "position must be 0 or greater")
        @Max(value = 999, message = "position must not exceed 999")
        Integer position
) {}
