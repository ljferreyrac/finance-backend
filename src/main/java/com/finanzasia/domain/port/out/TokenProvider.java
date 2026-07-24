package com.finanzasia.domain.port.out;

import com.finanzasia.domain.model.User;

import java.time.Duration;
import java.util.UUID;

/**
 * Output port: mints and reads the access/refresh token pair.
 *
 * <p>Deliberately exposes only plain types. Leaking a JWT library type here
 * would put the token format back into the application layer, which is the
 * coupling this port exists to remove. Token parsing that needs library types
 * stays inside the adapter.
 */
public interface TokenProvider {

    /** @return a signed short-lived access token for the given user */
    String generateAccessToken(User user);

    /** @return a signed long-lived refresh token carrying a fresh JTI */
    String generateRefreshToken(UUID userId);

    /**
     * Extracts the JTI claim, the id under which a refresh token is registered
     * in the {@link TokenStore}.
     */
    String extractJti(String token);

    /** Extracts the subject claim as the user id. */
    UUID extractUserId(String token);

    /** How long an issued refresh token stays valid; used as the store TTL. */
    Duration refreshTokenDuration();

    /** Access token lifetime in seconds, returned to clients on login. */
    long accessExpiresInSeconds();
}
