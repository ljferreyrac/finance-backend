package com.finanzasia.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateAccountRequest(

        @Size(max = 100)
        String name,

        @Size(max = 100)
        String bank,

        @DecimalMin("0")
        BigDecimal creditLimit,

        @Min(1) @Max(31)
        Integer closingDay,

        @Min(1) @Max(31)
        Integer dueDay,

        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "color must be a 6-digit hex code, e.g. #FF5733")
        String color,

        boolean isActive,

        UUID linkedAccountId
) {}
