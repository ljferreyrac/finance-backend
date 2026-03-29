package com.finanzasia.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Immutable value object representing a single transaction extracted by the AI
 * from a voice transcript. All fields except {@code amount} and {@code transactionDate}
 * may be null when the AI could not determine them with sufficient confidence.
 * The caller is expected to present this draft to the user for review before
 * committing it as a real transaction.
 */
public record TransactionDraft(
        TransactionType type,
        BigDecimal amount,
        String currency,
        UUID categoryId,
        String categoryName,
        UUID accountId,
        String accountName,
        String merchant,
        String description,
        LocalDate transactionDate,
        double confidence,
        List<UUID> tagIds
) {}
