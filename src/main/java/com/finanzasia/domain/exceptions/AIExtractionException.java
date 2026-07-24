package com.finanzasia.domain.exceptions;

/**
 * Covers both HTTP errors from the Gemini API and malformed JSON responses.
 */
public class AIExtractionException extends RuntimeException {

    public AIExtractionException(String message) {
        super(message);
    }

    public AIExtractionException(String message, Throwable cause) {
        super(message, cause);
    }
}
