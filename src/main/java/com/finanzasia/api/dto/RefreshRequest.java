package com.finanzasia.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Payload for rotating a refresh token")
public record RefreshRequest(

        @Schema(description = "The current refresh token to exchange for a new pair")
        @NotBlank
        String refreshToken
) {}
