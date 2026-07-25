package com.finanzasia.infrastructure.ai;

import com.finanzasia.domain.exceptions.AIExtractionException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * GroqTranscriptionService builds its own {@link org.springframework.web.client.RestClient}
 * internally from the injected {@code apiUrl} rather than taking one by constructor, so it
 * cannot be pointed at a MockRestServiceServer. A real local HTTP server (part of the JDK,
 * no extra dependency) stands in for the Groq API instead.
 */
class GroqTranscriptionServiceTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    private String startServer(int statusCode, String responseBody) throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> respond(exchange, statusCode, responseBody));
        server.start();
        return "http://localhost:" + server.getAddress().getPort();
    }

    private void respond(HttpExchange exchange, int statusCode, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    @Test
    @DisplayName("a successful response returns the transcribed text")
    void successfulResponseReturnsText() throws IOException {
        String apiUrl = startServer(200, "{\"text\":\"gaste veinte soles en el mercado\"}");
        GroqTranscriptionService service = new GroqTranscriptionService("test-key", apiUrl);

        String result = service.transcribe(new byte[]{1, 2, 3}, "audio.wav");

        assertThat(result).isEqualTo("gaste veinte soles en el mercado");
    }

    @Nested
    @DisplayName("empty or missing transcription text")
    class EmptyResponse {

        @Test
        @DisplayName("a response with a blank text field raises AIExtractionException")
        void blankTextThrows() throws IOException {
            String apiUrl = startServer(200, "{\"text\":\"   \"}");
            GroqTranscriptionService service = new GroqTranscriptionService("test-key", apiUrl);

            assertThatThrownBy(() -> service.transcribe(new byte[]{1}, "audio.wav"))
                    .isInstanceOf(AIExtractionException.class)
                    .hasMessageContaining("empty transcription");
        }

        @Test
        @DisplayName("a response with a null text field raises AIExtractionException")
        void nullTextThrows() throws IOException {
            String apiUrl = startServer(200, "{}");
            GroqTranscriptionService service = new GroqTranscriptionService("test-key", apiUrl);

            assertThatThrownBy(() -> service.transcribe(new byte[]{1}, "audio.wav"))
                    .isInstanceOf(AIExtractionException.class)
                    .hasMessageContaining("empty transcription");
        }
    }

    @Test
    @DisplayName("a server error surfaces as AIExtractionException, not the raw RestClientException")
    void serverErrorMapsToAiExtractionException() throws IOException {
        String apiUrl = startServer(500, "{\"error\":\"internal\"}");
        GroqTranscriptionService service = new GroqTranscriptionService("test-key", apiUrl);

        assertThatThrownBy(() -> service.transcribe(new byte[]{1}, "audio.wav"))
                .isInstanceOf(AIExtractionException.class)
                .hasMessageContaining("Groq transcription failed");
    }
}
