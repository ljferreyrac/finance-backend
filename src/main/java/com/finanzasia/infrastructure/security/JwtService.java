package com.finanzasia.infrastructure.security;

import com.finanzasia.domain.exceptions.InvalidTokenException;
import com.finanzasia.domain.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Creates and validates access and refresh tokens, both signed with HS256.
 * Access tokens carry {@code sub}, {@code email}. Refresh tokens carry {@code sub} and
 * a random {@code jti} used as the Redis key for per-token revocation.
 */
@Component
public class JwtService {

    private final SecretKey signingKey;
    private final long accessTokenExpiryMs;
    private final long refreshTokenExpiryMs;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-expiry-ms}") long accessTokenExpiryMs,
            @Value("${app.jwt.refresh-token-expiry-ms}") long refreshTokenExpiryMs) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiryMs = accessTokenExpiryMs;
        this.refreshTokenExpiryMs = refreshTokenExpiryMs;
    }

    public long accessExpiresInSeconds() {
        return accessTokenExpiryMs / 1000;
    }

    public java.time.Duration refreshTokenDuration() {
        return java.time.Duration.ofMillis(refreshTokenExpiryMs);
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(accessTokenExpiryMs)))
                .signWith(signingKey)
                .compact();
    }

    // The jti claim is a random UUID, stored in Redis to allow per-token revocation.
    public String generateRefreshToken(UUID userId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(refreshTokenExpiryMs)))
                .signWith(signingKey)
                .compact();
    }

    /** @throws InvalidTokenException if the token is malformed, expired, or has an invalid signature */
    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException ex) {
            throw new InvalidTokenException(ex.getMessage());
        }
    }

    /** @throws InvalidTokenException if the token cannot be parsed */
    public String extractJti(String token) {
        return parseToken(token).getId();
    }

    /** @throws InvalidTokenException if the token cannot be parsed or sub is missing */
    public UUID extractUserId(String token) {
        String subject = parseToken(token).getSubject();
        try {
            return UUID.fromString(subject);
        } catch (IllegalArgumentException ex) {
            throw new InvalidTokenException("subject is not a valid UUID: " + subject);
        }
    }
}
