package com.finanzasia.domain.model;

import java.util.List;

/**
 * Immutable value object carrying a single page of transactions
 * and the cursor needed to fetch the next page.
 */
public record TransactionPage(
        List<Transaction> items,
        String nextCursor,
        boolean hasMore
) {}
