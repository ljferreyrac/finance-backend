package com.finanzasia.api.dto;

import com.finanzasia.domain.model.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Schema(description = "Full transaction representation returned in API responses")
public record TransactionDTO(

        @Schema(description = "Transaction UUID") UUID id,
        @Schema(description = "Transaction type") TransactionType type,
        @Schema(description = "Amount", example = "150.00") BigDecimal amount,
        @Schema(description = "ISO currency code", example = "PEN") String currency,

        @Schema(description = "Account used for EXPENSE or INCOME (null for TRANSFER)")
        AccountSummaryDTO account,

        @Schema(description = "Source account for TRANSFER (null otherwise)")
        AccountSummaryDTO fromAccount,

        @Schema(description = "Destination account for TRANSFER (null otherwise)")
        AccountSummaryDTO toAccount,

        @Schema(description = "Category (EXPENSE only, null otherwise)")
        CategorySummaryDTO category,

        @Schema(description = "Merchant name", example = "Plaza Vea") String merchant,
        @Schema(description = "Free-text description") String description,
        @Schema(description = "Date of the transaction") LocalDate transactionDate,
        @Schema(description = "Creation timestamp") Instant createdAt,
        @Schema(description = "Tags attached to this transaction") List<TagDTO> tags,

        @Schema(description = "User-confirmed PEN equivalent for cross-currency transactions. "
                            + "Null for same-currency transactions.",
                example = "374.00")
        BigDecimal amountLocal,

        @Schema(description = "Effective rate (amountLocal / amount) stored for audit. "
                            + "Null for same-currency transactions.",
                example = "3.7400")
        BigDecimal exchangeRateApplied
) {}
