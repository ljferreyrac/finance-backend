package com.finanzasia.infrastructure.ai;

import com.finanzasia.domain.exceptions.AIExtractionException;
import com.finanzasia.domain.port.out.AudioTranscriptionPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Transcribes audio using the Groq Whisper API (OpenAI-compatible endpoint).
 * Model: whisper-large-v3, language fixed to Spanish for accuracy on Peruvian speech.
 */
@Service
public class GroqTranscriptionService implements AudioTranscriptionPort {

    private final String apiKey;
    private final RestClient restClient;

    public GroqTranscriptionService(
            @Value("${groq.api.key}") String apiKey,
            @Value("${groq.api.url}") String apiUrl) {
        this.apiKey = apiKey;
        this.restClient = RestClient.builder()
                .baseUrl(apiUrl)
                .build();
    }

    @Override
    public String transcribe(byte[] audioBytes, String filename) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        body.add("file", new NamedByteArrayResource(audioBytes, filename));
        body.add("model", "whisper-large-v3");
        body.add("language", "es");
        body.add("response_format", "json");

        try {
            GroqTranscriptionResponse response = restClient.post()
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(GroqTranscriptionResponse.class);

            if (response == null || response.text() == null || response.text().isBlank()) {
                throw new AIExtractionException("Groq returned an empty transcription");
            }
            return response.text();

        } catch (RestClientException e) {
            throw new AIExtractionException("Groq transcription failed: " + e.getMessage());
        }
    }

    // ByteArrayResource subclass that also exposes a filename so Spring sets the
    // correct Content-Disposition header in the multipart request.
    private static final class NamedByteArrayResource extends ByteArrayResource {
        private final String filename;

        NamedByteArrayResource(byte[] bytes, String filename) {
            super(bytes);
            this.filename = filename;
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }

    private record GroqTranscriptionResponse(String text) {}
}
