package com.finanzasia.api.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Response body for a single AI-extracted transaction draft.
 * All identifier fields are serialized as strings so clients do not need to
 * handle UUID types natively. {@code categoryId} and {@code accountId} may be
 * null when the AI could not match them to a known user resource.
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
