package com.finanzasia.domain.model;

import java.util.List;

/**
 * A page of transactions with their account and category references resolved.
 *
 * <p>The enriched counterpart of {@link TransactionPage}: the repository returns
 * the raw page, and the service turns it into this once, rather than the web
 * layer resolving references per item.
 */
public record TransactionDetailPage(
        List<TransactionDetail> items,
        String nextCursor,
        boolean hasMore
) {}
