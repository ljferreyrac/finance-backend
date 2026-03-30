package com.finanzasia.infrastructure.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finanzasia.domain.exceptions.AIExtractionException;
import com.finanzasia.domain.port.out.AIExtractionPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

/**
 * Calls the Google Gemini Flash API over plain HTTP using Spring's {@link RestClient}.
 * No Gemini SDK is used; the JSON request and response are handled manually via Jackson.
 *
 * <p>The prompt asks the model to return a raw JSON array so that
 * {@code generationConfig.responseMimeType = "application/json"} can be set,
 * reducing the risk of markdown-wrapped output.
 */
@Component
public class GeminiExtractionService implements AIExtractionPort {

    private static final Logger log = LoggerFactory.getLogger(GeminiExtractionService.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String apiUrl;

    public GeminiExtractionService(
            RestClient restClient,
            ObjectMapper objectMapper,
            @Value("${gemini.api.key}") String apiKey,
            @Value("${gemini.api.url}") String apiUrl) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
    }

    @Override
    public List<AITransactionRaw> extractFromText(
            String transcript,
            List<CategoryContext> categories,
            List<AccountContext> accounts,
            List<TagContext> tags,
            String userTimezone) {

        String prompt = buildPrompt(transcript, categories, accounts, tags, userTimezone);
        String requestBody = buildRequestBody(prompt);

        String responseBody;
        try {
            responseBody = restClient.post()
                    .uri(apiUrl + "?key=" + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientException ex) {
            log.error("Gemini API call failed: {}", ex.getMessage());
            throw new AIExtractionException("AI service unavailable. Please try again later.", ex);
        }

        return parseResponse(responseBody);
    }

    // ------------------------------------------------------------------
    // Private helpers
    // ------------------------------------------------------------------

    private String buildPrompt(
            String transcript,
            List<CategoryContext> categories,
            List<AccountContext> accounts,
            List<TagContext> tags,
            String userTimezone) {

        ZoneOffset offset = ZoneOffset.UTC;
        if (userTimezone != null && !userTimezone.isBlank()) {
            try {
                offset = ZoneOffset.of(userTimezone);
            } catch (Exception ex) {
                log.warn("Invalid userTimezone '{}', falling back to UTC", userTimezone);
            }
        }
        String today = LocalDate.now(offset).toString();
        String categoriesJson;
        String accountsJson;
        String tagsJson;
        try {
            categoriesJson = objectMapper.writeValueAsString(categories);
            accountsJson = objectMapper.writeValueAsString(accounts);
            tagsJson = objectMapper.writeValueAsString(tags);
        } catch (JsonProcessingException ex) {
            throw new AIExtractionException("Failed to serialize context for AI prompt.", ex);
        }

        return "You are a financial assistant for a Peruvian personal finance app.\n"
                + "Extract ALL transactions mentioned in the user's text.\n"
                + "Today's date is " + today + ".\n\n"
                + "Available categories (use the exact ID):\n"
                + categoriesJson + "\n\n"
                + "Available accounts (use the exact ID):\n"
                + accountsJson + "\n\n"
                + "Available tags (optional - use the exact ID if the user mentions any labels):\n"
                + tagsJson + "\n\n"
                + "Rules:\n"
                + "- \"soles\", \"S/\", \"PEN\" -> currency: \"PEN\". \"$\", \"dolares\" -> currency: \"USD\"\n"
                + "- If no currency mentioned, default to \"PEN\"\n"
                + "- If no date mentioned, use today\n"
                + "- If type not clear, default to \"EXPENSE\"\n"
                + "- Map category to the closest available category ID. If unsure, use null.\n"
                + "- Map account to the closest available account ID. If unsure, use null.\n"
                + "- If the user mentions a tag name or label, match it to the closest available tag ID and include it in tagIds. If none match, return an empty array.\n"
                + "- confidence: 0.0 to 1.0 based on how certain you are of each field\n\n"
                + "Respond ONLY with a valid JSON array, no markdown, no explanation:\n"
                + "[\n"
                + "  {\n"
                + "    \"type\": \"EXPENSE|INCOME|TRANSFER\",\n"
                + "    \"amount\": 0.00,\n"
                + "    \"currency\": \"PEN|USD\",\n"
                + "    \"categoryId\": \"uuid-or-null\",\n"
                + "    \"accountId\": \"uuid-or-null\",\n"
                + "    \"merchant\": \"string-or-null\",\n"
                + "    \"description\": \"string-or-null\",\n"
                + "    \"transactionDate\": \"YYYY-MM-DD\",\n"
                + "    \"confidence\": 0.0,\n"
                + "    \"tagIds\": []\n"
                + "  }\n"
                + "]\n\n"
                + "User said: \"" + transcript + "\"";
    }

    /**
     * Builds the Gemini API request body as a JSON string.
     * Uses {@code responseMimeType: "application/json"} to instruct the model to
     * return only a JSON array with no surrounding markdown.
     */
    private String buildRequestBody(String prompt) {
        try {
            Map<String, Object> requestMap = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(Map.of("text", prompt)))
                    ),
                    "generationConfig", Map.of(
                            "responseMimeType", "application/json"
                    )
            );
            return objectMapper.writeValueAsString(requestMap);
        } catch (JsonProcessingException ex) {
            throw new AIExtractionException("Failed to build Gemini API request body.", ex);
        }
    }

    /**
     * Extracts the text content from the Gemini response envelope and deserializes
     * it into the typed list of raw transaction records.
     */
    private List<AITransactionRaw> parseResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode textNode = root
                    .path("candidates")
                    .path(0)
                    .path("content")
                    .path("parts")
                    .path(0)
                    .path("text");

            if (textNode.isMissingNode()) {
                log.error("Unexpected Gemini response structure: {}", responseBody);
                throw new AIExtractionException("AI returned an unrecognised response format.");
            }

            String jsonArray = textNode.asText();
            return objectMapper.readValue(jsonArray, new TypeReference<List<AITransactionRaw>>() {});

        } catch (JsonProcessingException ex) {
            log.error("Failed to parse Gemini response: {}", ex.getMessage());
            throw new AIExtractionException("AI returned a response that could not be parsed.", ex);
        }
    }
}
