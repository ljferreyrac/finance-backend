package com.finanzasia.domain.model;

/**
 * Value object returned after a successful login or token refresh.
 * {@code accessExpiresIn} is expressed in seconds and is forwarded
 * directly to the API response so the client knows when to refresh.
 */
public record AuthTokens(
        String accessToken,
        String refreshToken,
        long accessExpiresIn) {
}
