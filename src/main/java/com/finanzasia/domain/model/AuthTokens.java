package com.finanzasia.domain.model;

/**
 * {@code accessExpiresIn} is expressed in seconds.
 */
public record AuthTokens(
        String accessToken,
        String refreshToken,
        long accessExpiresIn) {
}
