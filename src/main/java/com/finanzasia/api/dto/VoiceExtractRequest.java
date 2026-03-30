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
        String transcript,

        /**
         * UTC offset sent by the client (e.g. "-05:00", "+00:00").
         * Used to compute the user's local "today" when the AI resolves relative
         * date expressions such as "hoy" or "ayer". Defaults to UTC if absent.
         */
        @Size(max = 6, message = "userTimezone must be a UTC offset like +05:30")
        String userTimezone

) {}
