package com.finanzasia.domain.port.out;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/** Output port: short-lived refresh token registry backed by Redis. */
public interface TokenStore {

    /**
     * @param tokenId the JTI claim value (random UUID string)
     * @param ttl     how long the entry should live (matches the JWT expiry)
     */
    void storeRefreshToken(String tokenId, UUID userId, Duration ttl);

    /**
     * Looks up the user associated with the given token ID.
     * Returns {@link Optional#empty()} when the token does not exist or has
     * already been invalidated.
     */
    Optional<UUID> getUserIdForRefreshToken(String tokenId);

    void invalidateRefreshToken(String tokenId);

    /**
     * Revokes every active refresh token belonging to the given user,
     * logging them out of all devices. Used when refresh-token reuse is
     * detected (possible theft) and available for a future
     * "log out everywhere" feature.
     */
    void revokeAllForUser(UUID userId);
}
