package com.finanzasia.infrastructure.redis;

import com.finanzasia.domain.port.out.TokenStore;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Redis-backed {@link TokenStore}. Key schema:
 * {@code refresh:{tokenId} -> userId}, TTL matches JWT expiry so Redis self-evicts.
 * {@code user-refresh:{userId} -> set of tokenIds}, indexed to support revoking all
 * of a user's sessions at once ("log out everywhere"); its TTL is refreshed on every
 * store so it outlives the newest token.
 */
public class RedisTokenStore implements TokenStore {

    private static final String KEY_PREFIX = "refresh:";
    private static final String USER_INDEX_PREFIX = "user-refresh:";

    private final StringRedisTemplate redisTemplate;

    public RedisTokenStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void storeRefreshToken(String tokenId, UUID userId, Duration ttl) {
        redisTemplate.opsForValue().set(key(tokenId), userId.toString(), ttl);
        String indexKey = userIndexKey(userId);
        redisTemplate.opsForSet().add(indexKey, tokenId);
        redisTemplate.expire(indexKey, ttl);
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
        String userId = redisTemplate.opsForValue().get(key(tokenId));
        redisTemplate.delete(key(tokenId));
        if (userId != null) {
            redisTemplate.opsForSet().remove(userIndexKey(UUID.fromString(userId)), tokenId);
        }
    }

    @Override
    public void revokeAllForUser(UUID userId) {
        String indexKey = userIndexKey(userId);
        Set<String> tokenIds = redisTemplate.opsForSet().members(indexKey);
        if (tokenIds != null) {
            for (String tokenId : tokenIds) {
                redisTemplate.delete(key(tokenId));
            }
        }
        redisTemplate.delete(indexKey);
    }

    private String key(String tokenId) {
        return KEY_PREFIX + tokenId;
    }

    private String userIndexKey(UUID userId) {
        return USER_INDEX_PREFIX + userId;
    }
}
