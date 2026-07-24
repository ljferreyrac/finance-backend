package com.finanzasia.api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Fixed-window rate limiter backed by Redis (INCR + EXPIRE per window). Protects the
 * auth endpoints from brute force and the voice endpoints from cost abuse of the paid
 * Groq/Gemini APIs. Keyed by authenticated user when available, otherwise by client IP.
 * Fails open when Redis is unreachable so an infra outage never locks users out.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(RateLimitFilter.class);

    private record Rule(String bucket, int limit, Duration window) {
    }

    private static final Map<String, Rule> RULES = Map.of(
            "/api/v1/auth/login", new Rule("login", 10, Duration.ofMinutes(15)),
            "/api/v1/auth/register", new Rule("register", 5, Duration.ofHours(1)),
            "/api/v1/auth/refresh", new Rule("refresh", 60, Duration.ofMinutes(15)),
            "/api/v1/auth/logout", new Rule("logout", 60, Duration.ofMinutes(15)),
            "/api/v1/transactions/extract-voice", new Rule("voice", 30, Duration.ofHours(1)),
            "/api/v1/transactions/voice-audio", new Rule("voice", 30, Duration.ofHours(1)));

    private static final String KEY_PREFIX = "ratelimit:";

    private final StringRedisTemplate redisTemplate;

    public RateLimitFilter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        Rule rule = RULES.get(request.getRequestURI());
        if (rule == null || !"POST".equals(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = KEY_PREFIX + rule.bucket() + ":" + clientKey(request);

        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redisTemplate.expire(key, rule.window());
            }
            if (count != null && count > rule.limit()) {
                reject(response, key, rule);
                return;
            }
        } catch (DataAccessException ex) {
            // Fail open, see class javadoc.
            LOG.warn("Rate limiter unavailable, allowing request: {}", ex.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private void reject(HttpServletResponse response, String key, Rule rule) throws IOException {
        long retryAfterSeconds = rule.window().toSeconds();
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        if (ttl != null && ttl > 0) {
            retryAfterSeconds = ttl;
        }
        response.setStatus(429);
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
                "{\"error\":\"too_many_requests\",\"message\":\"Too many requests. Please try again later.\"}");
    }

    // Keyed per user so an abusive account can't dodge the limit by rotating IPs.
    private String clientKey(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return "user:" + principal.getId();
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return "ip:" + forwardedFor.split(",")[0].trim();
        }
        return "ip:" + request.getRemoteAddr();
    }
}
