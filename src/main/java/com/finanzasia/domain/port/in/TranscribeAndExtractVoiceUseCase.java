package com.finanzasia.domain.port.in;

import com.finanzasia.domain.model.TransactionDraft;

import java.util.List;
import java.util.UUID;

/**
 * Input port for the full audio-to-transactions pipeline:
 * raw audio -> Groq Whisper transcription -> Gemini extraction -> drafts.
 */
public interface TranscribeAndExtractVoiceUseCase {

    /**
     * @param userId       the authenticated user whose categories and accounts are used for context
     * @param filename     original filename with extension (e.g. "voice.m4a")
     * @param userTimezone UTC offset string sent by the client (e.g. "-05:00"); null defaults to UTC
     * @return an ordered list of drafts; never null, may be empty
     */
    List<TransactionDraft> transcribeAndExtract(
            UUID userId,
            byte[] audioBytes,
            String filename,
            String userTimezone);
}
