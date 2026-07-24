package com.finanzasia.api.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * IDs are serialized as strings so clients avoid native UUID handling.
 * {@code categoryId}/{@code accountId} are null when the AI found no matching user resource.
 */
public record TransactionDraftDTO(
        String type,
        BigDecimal amount,
        String currency,
        String categoryId,
        String categoryName,
        String accountId,
        String accountName,
        String merchant,
        String description,
        String transactionDate,
        double confidence,
        List<String> tagIds
) {}
