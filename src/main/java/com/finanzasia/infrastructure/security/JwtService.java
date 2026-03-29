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
 * Central JWT utility: creates and validates both access and refresh tokens.
 *
 * <p>Access token claims: {@code sub} (userId), {@code email}, {@code iat}, {@code exp}.
 * <p>Refresh token claims: {@code sub} (userId), {@code jti} (random UUID used as Redis key),
 * {@code iat}, {@code exp}.
 *
 * <p>Both tokens are signed with HS256.
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

    /** Returns the access token TTL in seconds for use in API responses. */
    public long accessExpiresInSeconds() {
        return accessTokenExpiryMs / 1000;
    }

    /** Returns the refresh token TTL as a {@link java.time.Duration}. */
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

    /**
     * Generates a refresh token for the given user.
     * A random UUID is used as the {@code jti} claim and stored in Redis
     * to allow per-token revocation.
     */
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

    /**
     * Parses and validates the token signature and expiry.
     *
     * @throws InvalidTokenException if the token is malformed, expired, or has an invalid signature
     */
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

    /**
     * Extracts the {@code jti} (JWT ID) claim from a refresh token.
     *
     * @throws InvalidTokenException if the token cannot be parsed
     */
    public String extractJti(String token) {
        return parseToken(token).getId();
    }

    /**
     * Extracts the {@code sub} claim and converts it to a {@link UUID}.
     *
     * @throws InvalidTokenException if the token cannot be parsed or sub is missing
     */
    public UUID extractUserId(String token) {
        String subject = parseToken(token).getSubject();
        try {
            return UUID.fromString(subject);
        } catch (IllegalArgumentException ex) {
            throw new InvalidTokenException("subject is not a valid UUID: " + subject);
        }
    }
}
