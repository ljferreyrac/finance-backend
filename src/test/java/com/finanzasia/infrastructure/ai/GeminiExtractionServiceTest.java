package com.finanzasia.infrastructure.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finanzasia.domain.exceptions.AIExtractionException;
import com.finanzasia.domain.port.out.AIExtractionPort.AITransactionRaw;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GeminiExtractionServiceTest {

    private static final String API_URL = "https://gemini.test/v1/generate";

    private MockRestServiceServer mockServer;
    private GeminiExtractionService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();
        service = new GeminiExtractionService(restClient, new ObjectMapper(), "test-api-key", API_URL);
    }

    private String geminiResponseWrapping(String innerJsonArray) {
        String escaped = innerJsonArray.replace("\"", "\\\"");
        return "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"" + escaped + "\"}]}}]}";
    }

    @Test
    @DisplayName("a well-formed Gemini response is parsed into the raw transaction list")
    void wellFormedResponseIsParsed() {
        String innerArray = "[{\"type\":\"EXPENSE\",\"amount\":25.50,\"currency\":\"PEN\","
                + "\"categoryId\":null,\"accountId\":null,\"merchant\":\"Wong\",\"description\":null,"
                + "\"transactionDate\":\"2026-03-28\",\"confidence\":0.9,\"tagIds\":[]}]";
        mockServer.expect(requestTo(API_URL))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withSuccess(geminiResponseWrapping(innerArray), MediaType.APPLICATION_JSON));

        List<AITransactionRaw> result = service.extractFromText(
                "gaste 25 soles en wong", List.of(), List.of(), List.of(), "-05:00");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).merchant()).isEqualTo("Wong");
        assertThat(result.get(0).currency()).isEqualTo("PEN");
    }

    @Test
    @DisplayName("a response missing the expected candidates/content/parts/text path raises AIExtractionException")
    void missingExpectedStructureThrows() {
        mockServer.expect(requestTo(API_URL))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> service.extractFromText(
                "algo", List.of(), List.of(), List.of(), null))
                .isInstanceOf(AIExtractionException.class)
                .hasMessageContaining("unrecognised response format");
    }

    @Test
    @DisplayName("a text field that is not valid JSON raises AIExtractionException")
    void unparsableInnerTextThrows() {
        mockServer.expect(requestTo(API_URL))
                .andRespond(withSuccess(geminiResponseWrapping("not a json array"), MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> service.extractFromText(
                "algo", List.of(), List.of(), List.of(), null))
                .isInstanceOf(AIExtractionException.class)
                .hasMessageContaining("could not be parsed");
    }

    @Test
    @DisplayName("a null/empty response body raises AIExtractionException rather than an unwrapped "
            + "IllegalArgumentException")
    void nullResponseBodyThrowsAiExtractionException() {
        mockServer.expect(requestTo(API_URL)).andRespond(withStatus(HttpStatus.NO_CONTENT));

        assertThatThrownBy(() -> service.extractFromText(
                "algo", List.of(), List.of(), List.of(), null))
                .isInstanceOf(AIExtractionException.class)
                .hasMessageContaining("could not be parsed");
    }

    @Test
    @DisplayName("a server error surfaces as AIExtractionException, not the raw RestClientException")
    void serverErrorMapsToAiExtractionException() {
        mockServer.expect(requestTo(API_URL)).andRespond(withServerError());

        assertThatThrownBy(() -> service.extractFromText(
                "algo", List.of(), List.of(), List.of(), null))
                .isInstanceOf(AIExtractionException.class)
                .hasMessageContaining("AI service unavailable");
    }

    @Nested
    @DisplayName("userTimezone handling")
    class UserTimezoneHandling {

        @Test
        @DisplayName("a null userTimezone still produces a valid request (defaults to UTC)")
        void nullTimezoneDefaultsToUtc() {
            mockServer.expect(requestTo(API_URL))
                    .andRespond(withSuccess(geminiResponseWrapping("[]"), MediaType.APPLICATION_JSON));

            List<AITransactionRaw> result = service.extractFromText(
                    "algo", List.of(), List.of(), List.of(), null);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("a blank userTimezone still produces a valid request (defaults to UTC)")
        void blankTimezoneDefaultsToUtc() {
            mockServer.expect(requestTo(API_URL))
                    .andRespond(withSuccess(geminiResponseWrapping("[]"), MediaType.APPLICATION_JSON));

            List<AITransactionRaw> result = service.extractFromText(
                    "algo", List.of(), List.of(), List.of(), "   ");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("a valid offset string is applied without falling back to UTC")
        void validOffsetIsApplied() {
            mockServer.expect(requestTo(API_URL))
                    .andRespond(withSuccess(geminiResponseWrapping("[]"), MediaType.APPLICATION_JSON));

            List<AITransactionRaw> result = service.extractFromText(
                    "algo", List.of(), List.of(), List.of(), "-05:00");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("an invalid, non-blank offset string falls back to UTC rather than failing the request")
        void invalidOffsetFallsBackToUtc() {
            mockServer.expect(requestTo(API_URL))
                    .andRespond(withSuccess(geminiResponseWrapping("[]"), MediaType.APPLICATION_JSON));

            List<AITransactionRaw> result = service.extractFromText(
                    "algo", List.of(), List.of(), List.of(), "not-a-valid-offset");

            assertThat(result).isEmpty();
        }
    }
}
