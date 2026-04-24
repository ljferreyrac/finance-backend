package com.finanzasia.api.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * All fields are optional. Omitted (null) fields retain their current values.
 * Passing null for color clears the existing color.
 * If name is provided it must be non-blank.
 */
public record UpdateTagRequest(

        @Size(min = 1, max = 50, message = "name must be between 1 and 50 characters")
        String name,

        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "color must be a valid hex color code, e.g. #FF5733")
        String color
) {}
