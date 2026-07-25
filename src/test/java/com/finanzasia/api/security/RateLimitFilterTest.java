package com.finanzasia.api.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        filter = new RateLimitFilter(redisTemplate);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private MockHttpServletRequest postRequest(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", uri);
        request.setRemoteAddr("203.0.113.5");
        return request;
    }

    @Test
    @DisplayName("a request to a non-rate-limited path passes through without touching Redis")
    void unrelatedPathPassesThrough() throws Exception {
        MockHttpServletRequest request = postRequest("/api/v1/accounts");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNotNull();
        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    @DisplayName("a GET to a rate-limited path passes through, since the rule only guards POST")
    void getRequestToLimitedPathPassesThrough() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNotNull();
        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    @DisplayName("the first request in a window increments the counter and sets the expiry")
    void firstRequestSetsExpiry() throws Exception {
        when(valueOperations.increment(anyString())).thenReturn(1L);
        MockHttpServletRequest request = postRequest("/api/v1/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        verify(redisTemplate).expire(anyString(), eq(java.time.Duration.ofMinutes(15)));
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("a subsequent request within limit does not re-set the expiry")
    void subsequentRequestWithinLimitSkipsExpiry() throws Exception {
        when(valueOperations.increment(anyString())).thenReturn(5L);
        MockHttpServletRequest request = postRequest("/api/v1/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        verify(redisTemplate, never()).expire(anyString(), any(java.time.Duration.class));
        assertThat(chain.getRequest()).isNotNull();
    }

    @Nested
    @DisplayName("over the limit")
    class OverLimit {

        @Test
        @DisplayName("responds 429 with Retry-After from the Redis TTL when available")
        void rejectsWithTtlBasedRetryAfter() throws Exception {
            when(valueOperations.increment(anyString())).thenReturn(11L);
            when(redisTemplate.getExpire(anyString(), eq(TimeUnit.SECONDS))).thenReturn(300L);
            MockHttpServletRequest request = postRequest("/api/v1/auth/login");
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            filter.doFilter(request, response, chain);

            assertThat(response.getStatus()).isEqualTo(429);
            assertThat(response.getHeader("Retry-After")).isEqualTo("300");
            assertThat(response.getContentAsString()).contains("too_many_requests");
            assertThat(chain.getRequest()).isNull();
        }

        @Test
        @DisplayName("falls back to the rule's window length when the Redis TTL is unavailable")
        void rejectsWithWindowBasedRetryAfterWhenTtlMissing() throws Exception {
            when(valueOperations.increment(anyString())).thenReturn(11L);
            when(redisTemplate.getExpire(anyString(), eq(TimeUnit.SECONDS))).thenReturn(null);
            MockHttpServletRequest request = postRequest("/api/v1/auth/login");
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            filter.doFilter(request, response, chain);

            assertThat(response.getStatus()).isEqualTo(429);
            assertThat(response.getHeader("Retry-After")).isEqualTo("900");
            assertThat(chain.getRequest()).isNull();
        }

        @Test
        @DisplayName("falls back to the rule's window length when the Redis TTL is zero or negative")
        void rejectsWithWindowBasedRetryAfterWhenTtlNonPositive() throws Exception {
            when(valueOperations.increment(anyString())).thenReturn(11L);
            when(redisTemplate.getExpire(anyString(), eq(TimeUnit.SECONDS))).thenReturn(0L);
            MockHttpServletRequest request = postRequest("/api/v1/auth/login");
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            filter.doFilter(request, response, chain);

            assertThat(response.getHeader("Retry-After")).isEqualTo("900");
        }
    }

    @Test
    @DisplayName("when Redis is unreachable, the filter fails open and lets the request through")
    void redisUnavailableFailsOpen() throws Exception {
        when(valueOperations.increment(anyString())).thenThrow(new QueryTimeoutException("timeout"));
        MockHttpServletRequest request = postRequest("/api/v1/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Nested
    @DisplayName("client key resolution")
    class ClientKeyResolution {

        @Test
        @DisplayName("an authenticated UserPrincipal is keyed by user id, not IP")
        void authenticatedRequestKeyedByUserId() throws Exception {
            UUID userId = UUID.randomUUID();
            UserPrincipal principal = new UserPrincipal(userId, "user@example.com");
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
            when(valueOperations.increment("ratelimit:login:user:" + userId)).thenReturn(1L);

            MockHttpServletRequest request = postRequest("/api/v1/auth/login");
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            filter.doFilter(request, response, chain);

            verify(valueOperations, times(1)).increment("ratelimit:login:user:" + userId);
        }

        @Test
        @DisplayName("an unauthenticated request with X-Forwarded-For is keyed by the first IP in the list")
        void unauthenticatedRequestUsesFirstForwardedIp() throws Exception {
            when(valueOperations.increment(anyString())).thenReturn(1L);
            MockHttpServletRequest request = postRequest("/api/v1/auth/login");
            request.addHeader("X-Forwarded-For", "198.51.100.1, 10.0.0.2");
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            filter.doFilter(request, response, chain);

            verify(valueOperations).increment("ratelimit:login:ip:198.51.100.1");
        }

        @Test
        @DisplayName("an unauthenticated request with no X-Forwarded-For falls back to the remote address")
        void unauthenticatedRequestFallsBackToRemoteAddr() throws Exception {
            when(valueOperations.increment(anyString())).thenReturn(1L);
            MockHttpServletRequest request = postRequest("/api/v1/auth/login");
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            filter.doFilter(request, response, chain);

            verify(valueOperations).increment("ratelimit:login:ip:203.0.113.5");
        }

        @Test
        @DisplayName("a blank X-Forwarded-For header is treated as absent")
        void blankForwardedForFallsBackToRemoteAddr() throws Exception {
            when(valueOperations.increment(anyString())).thenReturn(1L);
            MockHttpServletRequest request = postRequest("/api/v1/auth/login");
            request.addHeader("X-Forwarded-For", "   ");
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            filter.doFilter(request, response, chain);

            verify(valueOperations).increment("ratelimit:login:ip:203.0.113.5");
        }
    }
}
