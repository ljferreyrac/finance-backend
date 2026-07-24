package com.finanzasia.api.dto;

import com.finanzasia.domain.model.AccountType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateAccountRequest(

        @NotBlank @Size(max = 100)
        String name,

        @NotNull
        AccountType type,

        @Pattern(regexp = "^(PEN|USD)$", message = "currency must be PEN or USD")
        String currency,

        @DecimalMin(value = "-9999999.99", message = "initialBalance must not be less than -9,999,999.99")
        @DecimalMax(value = "9999999.99", message = "initialBalance must not exceed 9,999,999.99")
        @Digits(integer = 7, fraction = 2, message = "initialBalance must have at most 2 decimal places")
        BigDecimal initialBalance,

        @Size(max = 100)
        String bank,

        @DecimalMin("0")
        @Digits(integer = 7, fraction = 2)
        BigDecimal creditLimit,

        @Min(1) @Max(31)
        Integer closingDay,

        @Min(1) @Max(31)
        Integer dueDay,

        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "color must be a 6-digit hex code, e.g. #FF5733")
        String color,

        boolean isDefault,

        UUID linkedAccountId
) {}
