package com.finanzasia.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for {@code POST /api/v1/transactions/extract-voice}.
 * The transcript is the plain-text output from a speech-to-text engine on the client.
 */
public record VoiceExtractRequest(

        @NotBlank(message = "transcript must not be blank")
        @Size(max = 2000, message = "transcript must not exceed 2000 characters")
        String transcript

) {}
