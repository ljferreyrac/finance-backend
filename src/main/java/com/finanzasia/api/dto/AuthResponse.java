package com.finanzasia.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Token pair returned on successful authentication or refresh")
public record AuthResponse(

        @Schema(description = "Short-lived JWT access token (15 minutes by default)")
        String accessToken,

        @Schema(description = "Long-lived JWT refresh token (7 days by default). Store securely.")
        String refreshToken,

        @Schema(description = "Token type, always 'Bearer'", example = "Bearer")
        String tokenType,

        @Schema(description = "Number of seconds until the access token expires", example = "900")
        long expiresIn
) {}
