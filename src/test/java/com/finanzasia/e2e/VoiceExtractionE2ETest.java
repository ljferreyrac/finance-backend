package com.finanzasia.e2e;

import com.finanzasia.domain.exceptions.AIExtractionException;
import com.finanzasia.domain.model.TransactionDraft;
import com.finanzasia.domain.model.TransactionType;
import com.finanzasia.domain.port.in.ExtractTransactionsFromVoiceUseCase;
import com.finanzasia.domain.port.in.TranscribeAndExtractVoiceUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * The two voice endpoints end to end, with the AI use cases replaced by {@link MockitoBean}.
 *
 * <p>Phase 7 skipped these because the real implementations call Gemini and Groq, which cost
 * money per request and would make CI depend on a third-party service. Stubbing at the use-case
 * port keeps everything this suite actually cares about real - HTTP, JSON binding, multipart
 * parsing, {@code SecurityConfig}, the JWT filter, the rate limiter and
 * {@code GlobalExceptionHandler} - while the paid call itself is the only thing faked.
 */
class VoiceExtractionE2ETest extends AbstractE2ETest {

    @MockitoBean
    private ExtractTransactionsFromVoiceUseCase extractUseCase;

    @MockitoBean
    private TranscribeAndExtractVoiceUseCase transcribeAndExtractUseCase;

    private static final TransactionDraft SAMPLE_DRAFT = new TransactionDraft(
            TransactionType.EXPENSE,
            new BigDecimal("35.50"),
            "PEN",
            null,
            "food",
            null,
            "BCP Soles",
            "Wong",
            "compra en el supermercado",
            LocalDate.of(2026, 3, 15),
            0.92,
            List.of());

    @Test
    @DisplayName("extract-voice returns the drafts the AI produced, mapped to the DTO shape")
    void extractVoiceReturnsDrafts() {
        TokenPair user = registerAndLogin("voice-extract");
        when(extractUseCase.extract(any(), anyString(), any())).thenReturn(List.of(SAMPLE_DRAFT));

        ResponseEntity<List> response = restTemplate.exchange(
                "/api/v1/transactions/extract-voice", HttpMethod.POST,
                authorized(Map.of("transcript", "gaste 35.50 soles en Wong", "userTimezone", "-05:00"),
                        user.accessToken()),
                List.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        Map<String, Object> draft = (Map<String, Object>) response.getBody().get(0);
        assertThat(draft.get("type")).isEqualTo("EXPENSE");
        assertThat(new BigDecimal(draft.get("amount").toString())).isEqualByComparingTo("35.50");
        assertThat(draft.get("merchant")).isEqualTo("Wong");
        assertThat(draft.get("transactionDate")).isEqualTo("2026-03-15");
    }

    @Test
    @DisplayName("an empty draft list comes back as 200 with an empty array, not an error")
    void emptyDraftListIsNotAnError() {
        TokenPair user = registerAndLogin("voice-empty");
        when(extractUseCase.extract(any(), anyString(), any())).thenReturn(List.of());

        ResponseEntity<List> response = restTemplate.exchange(
                "/api/v1/transactions/extract-voice", HttpMethod.POST,
                authorized(Map.of("transcript", "hola"), user.accessToken()), List.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    @DisplayName("a blank transcript is rejected with 400 before the AI is ever called")
    void blankTranscriptReturns400() {
        TokenPair user = registerAndLogin("voice-blank");

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/transactions/extract-voice", HttpMethod.POST,
                authorized(Map.of("transcript", "   "), user.accessToken()), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("an AI failure surfaces as 502 with a generic message, never the provider's error")
    void aiFailureReturns502WithoutLeakingProviderDetail() {
        TokenPair user = registerAndLogin("voice-fail");
        when(extractUseCase.extract(any(), anyString(), any()))
                .thenThrow(new AIExtractionException("Gemini quota exceeded for project acme-1234"));

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/transactions/extract-voice", HttpMethod.POST,
                authorized(Map.of("transcript", "gaste 10 soles"), user.accessToken()), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody().get("title")).isEqualTo("AI Extraction Failed");
        // GlobalExceptionHandler logs the real cause but must not return it: the upstream message
        // can carry project ids, quota details and other provider internals.
        assertThat(response.getBody().toString()).doesNotContain("acme-1234").doesNotContain("quota");
    }

    @Test
    @DisplayName("extract-voice requires authentication")
    void extractVoiceRequiresAuthentication() {
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/v1/transactions/extract-voice",
                jsonBody(Map.of("transcript", "gaste 10 soles")), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("voice-audio accepts a multipart upload and returns the extracted drafts")
    void voiceAudioAcceptsMultipartUpload() {
        TokenPair user = registerAndLogin("voice-audio");
        when(transcribeAndExtractUseCase.transcribeAndExtract(any(), any(), anyString(), anyString()))
                .thenReturn(List.of(SAMPLE_DRAFT));

        ResponseEntity<List> response = restTemplate.exchange(
                "/api/v1/transactions/voice-audio", HttpMethod.POST,
                audioUpload("voice.m4a", "fake-audio-bytes", user.accessToken()), List.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    @DisplayName("a non-audio file extension is rejected with 400 before paying for transcription")
    void nonAudioExtensionIsRejected() {
        TokenPair user = registerAndLogin("voice-badext");

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/transactions/voice-audio", HttpMethod.POST,
                audioUpload("payload.exe", "not-audio", user.accessToken()), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("a file with no extension at all is rejected with 400")
    void extensionlessFileIsRejected() {
        TokenPair user = registerAndLogin("voice-noext");

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/transactions/voice-audio", HttpMethod.POST,
                audioUpload("recording", "not-audio", user.accessToken()), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("voice-audio requires authentication")
    void voiceAudioRequiresAuthentication() {
        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        parts.add("audio", namedResource("voice.m4a", "bytes"));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/transactions/voice-audio", HttpMethod.POST,
                new HttpEntity<>(parts, headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private HttpEntity<MultiValueMap<String, Object>> audioUpload(
            String filename, String content, String accessToken) {
        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        parts.add("audio", namedResource(filename, content));
        parts.add("timezone", "-05:00");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(accessToken);
        return new HttpEntity<>(parts, headers);
    }

    private ByteArrayResource namedResource(String filename, String content) {
        return new ByteArrayResource(content.getBytes(java.nio.charset.StandardCharsets.UTF_8)) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
    }
}
