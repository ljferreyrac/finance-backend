package com.finanzasia.infrastructure.redis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisTokenStoreTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private SetOperations<String, String> setOperations;

    private RedisTokenStore store;

    private static final String TOKEN_ID = "abc-123";
    private static final UUID USER_ID = UUID.randomUUID();
    private static final String KEY = "refresh:" + TOKEN_ID;
    private static final String USER_INDEX_KEY = "user-refresh:" + USER_ID;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(redisTemplate.opsForSet()).thenReturn(setOperations);
        store = new RedisTokenStore(redisTemplate);
    }

    @Test
    @DisplayName("storeRefreshToken writes the value, the user index and refreshes the index TTL")
    void storeRefreshTokenWritesValueAndIndex() {
        Duration ttl = Duration.ofDays(7);

        store.storeRefreshToken(TOKEN_ID, USER_ID, ttl);

        verify(valueOperations).set(KEY, USER_ID.toString(), ttl);
        verify(setOperations).add(USER_INDEX_KEY, TOKEN_ID);
        verify(redisTemplate).expire(USER_INDEX_KEY, ttl);
    }

    @Nested
    @DisplayName("getUserIdForRefreshToken")
    class GetUserIdForRefreshToken {

        @Test
        @DisplayName("returns the parsed UUID when the key exists")
        void returnsParsedUuidWhenPresent() {
            when(valueOperations.get(KEY)).thenReturn(USER_ID.toString());

            Optional<UUID> result = store.getUserIdForRefreshToken(TOKEN_ID);

            assertThat(result).contains(USER_ID);
        }

        @Test
        @DisplayName("returns empty when the key does not exist (expired or revoked)")
        void returnsEmptyWhenAbsent() {
            when(valueOperations.get(KEY)).thenReturn(null);

            assertThat(store.getUserIdForRefreshToken(TOKEN_ID)).isEmpty();
        }
    }

    @Nested
    @DisplayName("invalidateRefreshToken")
    class InvalidateRefreshToken {

        @Test
        @DisplayName("deletes the token and removes it from the user's index when the token existed")
        void deletesTokenAndRemovesFromIndexWhenPresent() {
            when(valueOperations.get(KEY)).thenReturn(USER_ID.toString());

            store.invalidateRefreshToken(TOKEN_ID);

            verify(redisTemplate).delete(KEY);
            verify(setOperations).remove(USER_INDEX_KEY, TOKEN_ID);
        }

        @Test
        @DisplayName("deletes the token but skips the index removal when the token was already gone")
        void skipsIndexRemovalWhenAlreadyGone() {
            when(valueOperations.get(KEY)).thenReturn(null);

            store.invalidateRefreshToken(TOKEN_ID);

            verify(redisTemplate).delete(KEY);
            verify(setOperations, never()).remove(eq(USER_INDEX_KEY), eq(TOKEN_ID));
        }
    }

    @Nested
    @DisplayName("revokeAllForUser")
    class RevokeAllForUser {

        @Test
        @DisplayName("deletes every token in the user's index, then the index itself")
        void deletesEveryTokenThenIndex() {
            String otherTokenId = "def-456";
            when(setOperations.members(USER_INDEX_KEY)).thenReturn(Set.of(TOKEN_ID, otherTokenId));

            store.revokeAllForUser(USER_ID);

            verify(redisTemplate).delete(KEY);
            verify(redisTemplate).delete("refresh:" + otherTokenId);
            verify(redisTemplate).delete(USER_INDEX_KEY);
        }

        @Test
        @DisplayName("a null member set (index already expired) still deletes the index key, no NPE")
        void nullMemberSetStillDeletesIndex() {
            when(setOperations.members(USER_INDEX_KEY)).thenReturn(null);

            store.revokeAllForUser(USER_ID);

            verify(redisTemplate).delete(USER_INDEX_KEY);
            verify(redisTemplate, never()).delete(KEY);
        }

        @Test
        @DisplayName("an empty member set deletes only the index key")
        void emptyMemberSetDeletesOnlyIndex() {
            when(setOperations.members(USER_INDEX_KEY)).thenReturn(Set.of());

            store.revokeAllForUser(USER_ID);

            verify(redisTemplate).delete(USER_INDEX_KEY);
            verify(redisTemplate, never()).delete(KEY);
        }
    }
}
