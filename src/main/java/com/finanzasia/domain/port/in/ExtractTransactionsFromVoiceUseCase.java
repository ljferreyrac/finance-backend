package com.finanzasia.domain.port.in;

import com.finanzasia.domain.model.TransactionDraft;

import java.util.List;
import java.util.UUID;

/**
 * Orchestrates loading user context, calling the AI backend, and mapping raw
 * AI output to typed domain drafts ready for user review.
 */
public interface ExtractTransactionsFromVoiceUseCase {

    /**
     * Extracts one or more transaction drafts from a plain-text voice transcript.
     *
     * @param userId       the authenticated user whose categories and accounts are used for context
     * @param transcript   the raw text transcribed from the user's voice input (max 2000 chars)
     * @param userTimezone UTC offset string sent by the client (e.g. "-05:00"); null defaults to UTC
     * @return an ordered list of drafts in the order the AI detected them; never null, may be empty
     */
    List<TransactionDraft> extract(UUID userId, String transcript, String userTimezone);
}
