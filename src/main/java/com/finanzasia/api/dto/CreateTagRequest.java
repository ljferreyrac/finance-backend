package com.finanzasia.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateTagRequest(

        @NotBlank
        @Size(max = 50, message = "name must not exceed 50 characters")
        String name,

        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "color must be a valid hex color code, e.g. #FF5733")
        String color
) {}
