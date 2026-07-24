package com.finanzasia.domain.port.out;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * The nested record types are pure data carriers with no framework dependencies.
 */
public interface AIExtractionPort {

    /**
     * The caller must pass the user's full category and account context so the
     * AI can resolve them by ID, avoiding an extra resolution step in the domain.
     *
     * @param userTimezone UTC offset string (e.g. "-05:00") used to compute the user's local today;
     *                     null or blank defaults to UTC
     * @return list of raw AI results, one per detected transaction
     */
    List<AITransactionRaw> extractFromText(
            String transcript,
            List<CategoryContext> categories,
            List<AccountContext> accounts,
            List<TagContext> tags,
            String userTimezone);

    record CategoryContext(UUID id, String name) {}

    record AccountContext(UUID id, String name, String bank, String currency) {}

    record TagContext(UUID id, String name) {}

    /**
     * Field types are strings/primitives because they come directly from JSON
     * and have not yet been validated against the domain model.
     */
    record AITransactionRaw(
            String type,
            BigDecimal amount,
            String currency,
            String categoryId,
            String accountId,
            String merchant,
            String description,
            String transactionDate,
            double confidence,
            List<String> tagIds
    ) {}
}
