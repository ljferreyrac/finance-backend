package com.finanzasia.api.controller;

import com.finanzasia.api.security.UserPrincipal;
import com.finanzasia.domain.exceptions.AIExtractionException;
import com.finanzasia.domain.model.TransactionDraft;
import com.finanzasia.domain.model.TransactionType;
import com.finanzasia.domain.port.in.AuthenticateAccessTokenUseCase;
import com.finanzasia.domain.port.in.ExtractTransactionsFromVoiceUseCase;
import com.finanzasia.domain.port.in.TranscribeAndExtractVoiceUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VoiceExtractionController.class)
class VoiceExtractionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExtractTransactionsFromVoiceUseCase extractUseCase;

    @MockitoBean
    private TranscribeAndExtractVoiceUseCase transcribeAndExtractUseCase;

    @MockitoBean
    private AuthenticateAccessTokenUseCase authenticateAccessToken;

    @MockitoBean
    private StringRedisTemplate redisTemplate;

    private UserPrincipal principal;
    private static final UUID USER_ID = UUID.randomUUID();

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        principal = new UserPrincipal(USER_ID, "user@example.com");

        // Both voice routes share the "voice" rate-limit bucket, so RateLimitFilter (a plain
        // servlet filter in this slice) touches Redis on every request; a null increment()
        // result is the same as an unreachable Redis, so RateLimitFilter fails open.
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    private TransactionDraft fullDraft() {
        return new TransactionDraft(TransactionType.EXPENSE, new BigDecimal("25.50"), "PEN",
                UUID.randomUUID(), "Comida", UUID.randomUUID(), "BCP Soles", "Wong", "groceries",
                LocalDate.of(2026, 3, 28), 0.9, List.of(UUID.randomUUID()));
    }

    private TransactionDraft emptyDraft() {
        return new TransactionDraft(null, new BigDecimal("25.50"), "PEN",
                null, null, null, null, null, null, null, 0.5, null);
    }

    @Nested
    @DisplayName("POST /api/v1/transactions/extract-voice")
    class ExtractFromVoice {

        @Test
        @DisplayName("returns 200 with a fully populated draft mapped to strings")
        void fullyPopulatedDraftMapsIdsToStrings() throws Exception {
            TransactionDraft draft = fullDraft();
            when(extractUseCase.extract(eq(USER_ID), anyString(), any())).thenReturn(List.of(draft));

            mockMvc.perform(post("/api/v1/transactions/extract-voice")
                            .with(user(principal))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"transcript\":\"gaste 25 soles en wong\",\"userTimezone\":\"-05:00\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].type").value("EXPENSE"))
                    .andExpect(jsonPath("$[0].categoryId").value(draft.categoryId().toString()))
                    .andExpect(jsonPath("$[0].accountId").value(draft.accountId().toString()))
                    .andExpect(jsonPath("$[0].transactionDate").value("2026-03-28"))
                    .andExpect(jsonPath("$[0].tagIds[0]").value(draft.tagIds().get(0).toString()));
        }

        @Test
        @DisplayName("returns 200 with an empty draft's optional fields mapped to null")
        void emptyDraftMapsOptionalFieldsToNull() throws Exception {
            when(extractUseCase.extract(eq(USER_ID), anyString(), any())).thenReturn(List.of(emptyDraft()));

            mockMvc.perform(post("/api/v1/transactions/extract-voice")
                            .with(user(principal))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"transcript\":\"algo\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].type").doesNotExist())
                    .andExpect(jsonPath("$[0].categoryId").doesNotExist())
                    .andExpect(jsonPath("$[0].accountId").doesNotExist())
                    .andExpect(jsonPath("$[0].transactionDate").doesNotExist())
                    .andExpect(jsonPath("$[0].tagIds").isEmpty());
        }

        @Test
        @DisplayName("a blank transcript fails bean validation with 400")
        void blankTranscriptReturns400() throws Exception {
            mockMvc.perform(post("/api/v1/transactions/extract-voice")
                            .with(user(principal))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"transcript\":\"\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("an AI failure surfaces as 502 via GlobalExceptionHandler")
        void aiFailureReturns502() throws Exception {
            when(extractUseCase.extract(any(), any(), any()))
                    .thenThrow(new AIExtractionException("Gemini unavailable"));

            mockMvc.perform(post("/api/v1/transactions/extract-voice")
                            .with(user(principal))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"transcript\":\"algo\"}"))
                    .andExpect(status().isBadGateway());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/transactions/voice-audio")
    class TranscribeAndExtract {

        @Test
        @DisplayName("a valid audio file returns 200 with the extracted drafts")
        void validAudioReturns200() throws Exception {
            MockMultipartFile audio = new MockMultipartFile("audio", "voice.m4a", "audio/mp4", new byte[]{1, 2, 3});
            when(transcribeAndExtractUseCase.transcribeAndExtract(eq(USER_ID), any(), eq("voice.m4a"), eq("UTC")))
                    .thenReturn(List.of(fullDraft()));

            mockMvc.perform(multipart("/api/v1/transactions/voice-audio")
                            .file(audio)
                            .with(user(principal))
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].type").value("EXPENSE"));
        }

        @Test
        @DisplayName("a timezone param is forwarded instead of the UTC default")
        void timezoneParamIsForwarded() throws Exception {
            MockMultipartFile audio = new MockMultipartFile("audio", "voice.mp3", "audio/mpeg", new byte[]{1});
            when(transcribeAndExtractUseCase.transcribeAndExtract(any(), any(), any(), any()))
                    .thenReturn(List.of());

            mockMvc.perform(multipart("/api/v1/transactions/voice-audio")
                            .file(audio)
                            .param("timezone", "-05:00")
                            .with(user(principal))
                            .with(csrf()))
                    .andExpect(status().isOk());

            org.mockito.Mockito.verify(transcribeAndExtractUseCase)
                    .transcribeAndExtract(USER_ID, new byte[]{1}, "voice.mp3", "-05:00");
        }

        @Test
        @DisplayName("an empty audio file returns 400 without calling the use case")
        void emptyAudioReturns400() throws Exception {
            MockMultipartFile audio = new MockMultipartFile("audio", "voice.m4a", "audio/mp4", new byte[0]);

            mockMvc.perform(multipart("/api/v1/transactions/voice-audio")
                            .file(audio)
                            .with(user(principal))
                            .with(csrf()))
                    .andExpect(status().isBadRequest());

            org.mockito.Mockito.verify(transcribeAndExtractUseCase, org.mockito.Mockito.never())
                    .transcribeAndExtract(any(), any(), any(), any());
        }

        @Test
        @DisplayName("a disallowed file extension returns 400 without calling the use case")
        void disallowedExtensionReturns400() throws Exception {
            MockMultipartFile audio = new MockMultipartFile("audio", "voice.exe", "application/octet-stream",
                    new byte[]{1});

            mockMvc.perform(multipart("/api/v1/transactions/voice-audio")
                            .file(audio)
                            .with(user(principal))
                            .with(csrf()))
                    .andExpect(status().isBadRequest());

            org.mockito.Mockito.verify(transcribeAndExtractUseCase, org.mockito.Mockito.never())
                    .transcribeAndExtract(any(), any(), any(), any());
        }

        @Test
        @DisplayName("a filename with no extension at all returns 400")
        void noExtensionReturns400() throws Exception {
            MockMultipartFile audio = new MockMultipartFile("audio", "voicefile", "audio/mp4", new byte[]{1});

            mockMvc.perform(multipart("/api/v1/transactions/voice-audio")
                            .file(audio)
                            .with(user(principal))
                            .with(csrf()))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("a filename ending in a bare dot returns 400")
        void trailingDotReturns400() throws Exception {
            MockMultipartFile audio = new MockMultipartFile("audio", "voice.", "audio/mp4", new byte[]{1});

            mockMvc.perform(multipart("/api/v1/transactions/voice-audio")
                            .file(audio)
                            .with(user(principal))
                            .with(csrf()))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("a null original filename defaults to voice.m4a, which is an allowed extension")
        void nullFilenameDefaultsToAllowedExtension() throws Exception {
            // MockMvc's multipart request parsing does not preserve a null originalFilename through
            // the servlet layer (it round-trips to ""), so this branch is exercised via a direct
            // controller invocation instead, the same approach as the IOException case below.
            MultipartFile audio = mock(MultipartFile.class);
            when(audio.isEmpty()).thenReturn(false);
            when(audio.getOriginalFilename()).thenReturn(null);
            when(audio.getBytes()).thenReturn(new byte[]{1, 2, 3});
            when(transcribeAndExtractUseCase.transcribeAndExtract(eq(USER_ID), any(), eq("voice.m4a"), eq("UTC")))
                    .thenReturn(List.of());

            VoiceExtractionController controller =
                    new VoiceExtractionController(extractUseCase, transcribeAndExtractUseCase);

            var response = controller.transcribeAndExtract(audio, "UTC", principal);

            org.assertj.core.api.Assertions.assertThat(response.getStatusCode().value()).isEqualTo(200);
        }

        @Test
        @DisplayName("an IOException reading the upload wraps as AIExtractionException")
        void ioExceptionOnReadWrapsAsAiExtractionException() throws Exception {
            // MockMvc's multipart support parses real request parts, so there is no way to make
            // getBytes() throw through the HTTP layer; the controller is invoked directly instead.
            MultipartFile audio = mock(MultipartFile.class);
            when(audio.isEmpty()).thenReturn(false);
            when(audio.getBytes()).thenThrow(new IOException("disk full"));

            VoiceExtractionController controller =
                    new VoiceExtractionController(extractUseCase, transcribeAndExtractUseCase);

            org.assertj.core.api.Assertions.assertThatThrownBy(
                            () -> controller.transcribeAndExtract(audio, "UTC", principal))
                    .isInstanceOf(AIExtractionException.class)
                    .hasMessageContaining("disk full");
        }
    }
}
