package com.finanzasia.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request body for POST /api/v1/categories.
 */
@Schema(description = "Payload for creating a new category")
public record CreateCategoryRequest(

        @Schema(description = "Category display name", example = "Comida")
        @NotBlank(message = "name is required")
        @Size(max = 100, message = "name must not exceed 100 characters")
        String name,

        @Schema(description = "Hex color code for UI display. Must be a 6-digit hex value prefixed with #.",
                example = "#FF5733")
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$",
                 message = "color must be a valid 6-digit hex color code (e.g. #FF5733)")
        String color,

        @Schema(description = "Icon identifier for UI display. Accepts a single emoji or word key.",
                example = "🍔")
        @Pattern(regexp = "^[\\p{L}\\p{N}\\p{So}\\p{Sk}\\-_]{1,50}$",
                 message = "icon must be a single emoji or a word of up to 50 characters")
        String icon,

        @Schema(description = "Whether this should become the user's default category")
        boolean isDefault
) {}
