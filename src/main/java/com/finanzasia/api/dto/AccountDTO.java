package com.finanzasia.api.dto;

import com.finanzasia.domain.model.AccountType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Full account representation returned in API responses")
public record AccountDTO(

        @Schema(description = "Account UUID") UUID id,
        @Schema(description = "Display name", example = "BCP Soles") String name,
        @Schema(description = "Account type") AccountType type,
        @Schema(description = "Bank name", example = "BCP") String bank,
        @Schema(description = "ISO currency code", example = "PEN") String currency,
        @Schema(description = "Current balance", example = "1500.00") BigDecimal currentBalance,
        @Schema(description = "Credit limit (CREDIT_CARD only)", example = "5000.00") BigDecimal creditLimit,
        @Schema(description = "Statement closing day (CREDIT_CARD only)", example = "6") Integer closingDay,
        @Schema(description = "Payment due day (CREDIT_CARD only)", example = "26") Integer dueDay,
        @Schema(description = "Hex color for UI", example = "#0066CC") String color,
        @Schema(description = "Whether this is the user's default account") boolean isDefault,
        @Schema(description = "Whether the account is active") boolean isActive,
        @Schema(description = "Number of non-deleted transactions linked to this account") long transactionCount,
        @Schema(description = "Parent bank account UUID whose balance is adjusted when this account is used")
        UUID linkedAccountId,
        @Schema(description = "Remaining credit available (CREDIT_CARD only): creditLimit minus absolute"
                + " outstanding balance. Null for non-credit accounts.",
                example = "3500.00")
        BigDecimal availableCredit
) {}
