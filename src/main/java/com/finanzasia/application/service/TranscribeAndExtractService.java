package com.finanzasia.application.service;

import com.finanzasia.domain.model.TransactionDraft;
import com.finanzasia.domain.port.in.ExtractTransactionsFromVoiceUseCase;
import com.finanzasia.domain.port.in.TranscribeAndExtractVoiceUseCase;
import com.finanzasia.domain.port.out.AudioTranscriptionPort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Orchestrates the two-step voice pipeline:
 * 1. Transcribe audio bytes via {@link AudioTranscriptionPort} (Groq Whisper).
 * 2. Extract transaction drafts from the transcript via {@link ExtractTransactionsFromVoiceUseCase} (Gemini).
 */
@Service
public class TranscribeAndExtractService implements TranscribeAndExtractVoiceUseCase {

    private final AudioTranscriptionPort transcriptionPort;
    private final ExtractTransactionsFromVoiceUseCase extractUseCase;

    public TranscribeAndExtractService(
            AudioTranscriptionPort transcriptionPort,
            ExtractTransactionsFromVoiceUseCase extractUseCase) {
        this.transcriptionPort = transcriptionPort;
        this.extractUseCase = extractUseCase;
    }

    @Override
    public List<TransactionDraft> transcribeAndExtract(
            UUID userId,
            byte[] audioBytes,
            String filename,
            String userTimezone) {

        String transcript = transcriptionPort.transcribe(audioBytes, filename);
        return extractUseCase.extract(userId, transcript, userTimezone);
    }
}
