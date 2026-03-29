package com.finanzasia.domain.port.out;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/** Output port: short-lived refresh token registry backed by Redis. */
public interface TokenStore {

    /**
     * Stores a refresh token identifier with its owning user and TTL.
     *
     * @param tokenId the JTI claim value (random UUID string)
     * @param userId  the owner of the token
     * @param ttl     how long the entry should live (matches the JWT expiry)
     */
    void storeRefreshToken(String tokenId, UUID userId, Duration ttl);

    /**
     * Looks up the user associated with the given token ID.
     * Returns {@link Optional#empty()} when the token does not exist or has
     * already been invalidated.
     */
    Optional<UUID> getUserIdForRefreshToken(String tokenId);

    /** Removes the token from the store, effectively revoking it. */
    void invalidateRefreshToken(String tokenId);
}
