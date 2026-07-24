package com.finanzasia.domain.model;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Cursor-based pagination uses {@code cursorDate} and {@code cursorId} from the
 * last item of the previous page.
 */
public record TransactionFilter(
        UUID userId,
        TransactionType type,
        UUID accountId,
        UUID categoryId,
        LocalDate from,
        LocalDate to,
        String currency,
        UUID tagId,
        LocalDate cursorDate,
        UUID cursorId,
        int limit
) {}
