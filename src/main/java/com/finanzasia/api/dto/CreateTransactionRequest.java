package com.finanzasia.api.dto;

import com.finanzasia.domain.model.TransactionType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public record CreateTransactionRequest(

        @NotNull
        TransactionType type,

        @NotNull
        @DecimalMin(value = "0.01", message = "amount must be at least 0.01")
        @DecimalMax(value = "9999999.99", message = "amount must not exceed 9999999.99")
        BigDecimal amount,

        @NotBlank
        @Pattern(regexp = "^(PEN|USD)$", message = "currency must be PEN or USD")
        String currency,

        /** Required when type is EXPENSE or INCOME. */
        UUID accountId,

        /** Required when type is TRANSFER. */
        UUID fromAccountId,

        /** Required when type is TRANSFER. */
        UUID toAccountId,

        /** Required when type is EXPENSE. */
        UUID categoryId,

        @Size(max = 255)
        String merchant,

        @Size(max = 2000)
        String description,

        @NotNull
        LocalDate transactionDate,

        /** Optional list of tag IDs to attach. Null and empty list both mean no tags. Max 20 tags. */
        @Size(max = 20, message = "a transaction may have at most 20 tags")
        List<UUID> tagIds,

        /**
         * User-confirmed PEN equivalent for cross-currency transactions (e.g. a USD charge on a
         * PEN account). The mobile pre-fills this from the auto-calculated value (amount * sell rate)
         * but the user may edit it to match their actual bank charge before confirming.
         * Null for same-currency transactions or when the auto-calculated value is accepted.
         */
        @DecimalMin(value = "0.01", message = "amountLocal must be at least 0.01")
        @DecimalMax(value = "9999999.99", message = "amountLocal must not exceed 9999999.99")
        BigDecimal amountLocal
) {
    public CreateTransactionRequest {
        tagIds = (tagIds != null) ? tagIds : Collections.emptyList();
    }
}
