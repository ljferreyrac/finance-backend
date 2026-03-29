package com.finanzasia.infrastructure.redis;

import com.finanzasia.domain.port.out.TokenStore;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Redis-backed implementation of {@link TokenStore}.
 * Each refresh token is stored as:
 *   key   = "refresh:{tokenId}"
 *   value = userId (UUID string)
 * with a TTL matching the JWT expiry so Redis self-evicts expired entries.
 */
public class RedisTokenStore implements TokenStore {

    private static final String KEY_PREFIX = "refresh:";

    private final StringRedisTemplate redisTemplate;

    public RedisTokenStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void storeRefreshToken(String tokenId, UUID userId, Duration ttl) {
        redisTemplate.opsForValue().set(key(tokenId), userId.toString(), ttl);
    }

    @Override
    public Optional<UUID> getUserIdForRefreshToken(String tokenId) {
        String value = redisTemplate.opsForValue().get(key(tokenId));
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(UUID.fromString(value));
    }

    @Override
    public void invalidateRefreshToken(String tokenId) {
        redisTemplate.delete(key(tokenId));
    }

    private String key(String tokenId) {
        return KEY_PREFIX + tokenId;
    }
}
