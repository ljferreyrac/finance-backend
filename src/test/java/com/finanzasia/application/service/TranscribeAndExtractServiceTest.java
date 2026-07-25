package com.finanzasia.application.service;

import com.finanzasia.domain.model.TransactionDraft;
import com.finanzasia.domain.model.TransactionType;
import com.finanzasia.domain.port.in.ExtractTransactionsFromVoiceUseCase;
import com.finanzasia.domain.port.out.AudioTranscriptionPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TranscribeAndExtractServiceTest {

    @Mock
    private AudioTranscriptionPort transcriptionPort;

    @Mock
    private ExtractTransactionsFromVoiceUseCase extractUseCase;

    @InjectMocks
    private TranscribeAndExtractService service;

    private UUID userId;
    private byte[] audio;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        audio = new byte[] {1, 2, 3};
    }

    @Test
    void transcribesThenExtractsAndReturnsTheDrafts() {
        List<TransactionDraft> drafts = List.of(draft());
        when(transcriptionPort.transcribe(audio, "nota.m4a")).thenReturn("gaste 50 soles en Wong");
        when(extractUseCase.extract(userId, "gaste 50 soles en Wong", "America/Lima"))
                .thenReturn(drafts);

        List<TransactionDraft> result =
                service.transcribeAndExtract(userId, audio, "nota.m4a", "America/Lima");

        assertThat(result).isSameAs(drafts);
    }

    @Test
    void feedsTheTranscriptToExtractionRatherThanTheRawAudio() {
        when(transcriptionPort.transcribe(any(), anyString())).thenReturn("un cafe 12 soles");
        when(extractUseCase.extract(any(), anyString(), anyString())).thenReturn(List.of());

        service.transcribeAndExtract(userId, audio, "nota.m4a", "America/Lima");

        // Order matters: extraction is meaningless before the audio is transcribed.
        InOrder order = inOrder(transcriptionPort, extractUseCase);
        order.verify(transcriptionPort).transcribe(audio, "nota.m4a");
        order.verify(extractUseCase).extract(userId, "un cafe 12 soles", "America/Lima");
    }

    @Test
    void passesTheTimezoneThroughSoRelativeDatesResolveLocally() {
        when(transcriptionPort.transcribe(any(), anyString())).thenReturn("ayer pague el gas");
        when(extractUseCase.extract(any(), anyString(), anyString())).thenReturn(List.of());

        service.transcribeAndExtract(userId, audio, "nota.m4a", "America/Lima");

        verify(extractUseCase).extract(userId, "ayer pague el gas", "America/Lima");
    }

    @Test
    void returnsEmptyWhenExtractionFindsNothing() {
        when(transcriptionPort.transcribe(any(), anyString())).thenReturn("ruido de fondo");
        when(extractUseCase.extract(any(), anyString(), anyString())).thenReturn(List.of());

        List<TransactionDraft> result =
                service.transcribeAndExtract(userId, audio, "nota.m4a", "America/Lima");

        assertThat(result).isEmpty();
    }

    @Test
    void doesNotAttemptExtractionWhenTranscriptionFails() {
        when(transcriptionPort.transcribe(any(), anyString()))
                .thenThrow(new RuntimeException("Groq unavailable"));

        assertThatThrownBy(() ->
                service.transcribeAndExtract(userId, audio, "nota.m4a", "America/Lima"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Groq unavailable");

        verify(extractUseCase, never()).extract(any(), anyString(), anyString());
    }

    private TransactionDraft draft() {
        return new TransactionDraft(
                TransactionType.EXPENSE,
                new BigDecimal("50.00"),
                "PEN",
                null,
                "food",
                null,
                null,
                "Wong",
                null,
                LocalDate.of(2026, 7, 24),
                0.92,
                List.of());
    }
}
