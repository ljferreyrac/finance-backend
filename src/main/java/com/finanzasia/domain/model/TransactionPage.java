package com.finanzasia.domain.model;

import java.util.List;

public record TransactionPage(
        List<Transaction> items,
        String nextCursor,
        boolean hasMore
) {}
