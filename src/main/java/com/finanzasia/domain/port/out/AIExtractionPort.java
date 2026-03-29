package com.finanzasia.domain.port.out;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Output port for AI-powered text extraction.
 * The infrastructure layer provides the concrete implementation; the domain
 * layer only depends on this interface and the nested record types, which
 * are pure data carriers with no framework dependencies.
 */
public interface AIExtractionPort {

    /**
     * Sends a transcript to the AI model and returns raw extraction results.
     * The caller must pass the user's full category and account context so the
     * AI can resolve them by ID, avoiding an extra resolution step in the domain.
     *
     * @param transcript the plain-text voice content to analyse
     * @param categories all categories available to the user
     * @param accounts   all accounts available to the user
     * @return list of raw AI results, one per detected transaction
     */
    List<AITransactionRaw> extractFromText(
            String transcript,
            List<CategoryContext> categories,
            List<AccountContext> accounts,
            List<TagContext> tags);

    record CategoryContext(UUID id, String name) {}

    record AccountContext(UUID id, String name, String bank, String currency) {}

    record TagContext(UUID id, String name) {}

    /**
     * Raw AI response for a single detected transaction.
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
