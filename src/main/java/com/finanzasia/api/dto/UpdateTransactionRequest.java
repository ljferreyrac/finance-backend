package com.finanzasia.api.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Transaction type is immutable after creation and is intentionally absent here.
 * Any client-supplied "type" field will be ignored -- the existing type is always preserved.
 *
 * For tags: null means "do not change the current tag set"; an empty list means "remove all tags".
 */
public record UpdateTransactionRequest(

        @DecimalMin(value = "0.01", message = "amount must be at least 0.01")
        @DecimalMax(value = "9999999.99", message = "amount must not exceed 9999999.99")
        BigDecimal amount,

        @Pattern(regexp = "^(PEN|USD)$", message = "currency must be PEN or USD")
        String currency,

        UUID accountId,
        UUID fromAccountId,
        UUID toAccountId,
        UUID categoryId,

        @Size(max = 255)
        String merchant,

        @Size(max = 2000)
        String description,

        LocalDate transactionDate,

        /** Null = keep existing tags; empty list = remove all tags. Max 20 tags. */
        @Size(max = 20, message = "a transaction may have at most 20 tags")
        List<UUID> tagIds,

        /**
         * Updated user-confirmed PEN equivalent for cross-currency transactions.
         * Null means "recompute automatically using today's sell rate".
         */
        @DecimalMin(value = "0.01", message = "amountLocal must be at least 0.01")
        @DecimalMax(value = "9999999.99", message = "amountLocal must not exceed 9999999.99")
        BigDecimal amountLocal
) {}
